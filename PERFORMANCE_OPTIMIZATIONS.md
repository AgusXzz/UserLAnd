# Performance Optimization Recommendations for Termux Terminal Emulator

## Executive Summary

This document outlines key performance optimization opportunities identified in the Termux terminal emulator codebase. The recommendations focus on reducing CPU usage, improving rendering performance, and optimizing memory access patterns.

---

## 1. WcWidth Character Width Lookup Optimization

### Current Issue
The `WcWidth.width()` method performs binary search on two large tables (`ZERO_WIDTH` and `WIDE_EASTASIAN`) for every character width calculation. This is called frequently during:
- Text rendering (every character on screen)
- Text input processing
- Screen resizing operations

### Recommendation: Implement Multi-Level Lookup Table

**Priority: HIGH**

Replace the binary search with a hybrid approach:
1. **Direct lookup table** for common characters (ASCII and Latin-1): O(1)
2. **Segmented lookup** for BMP (Basic Multilingual Plane): O(1) with small constant
3. **Binary search fallback** only for rare supplementary characters

```java
// Proposed implementation structure:
private static final byte[] DIRECT_WIDTH_LOOKUP = new byte[4096]; // For U+0000 to U+0FFF
private static final byte[][] BMP_SEGMENT_LOOKUP = new byte[256][]; // For U+1000 to U+FFFF

public static int width(int ucs) {
    // Fast path for ASCII (covers 80%+ of terminal text)
    if (ucs < 128) {
        return (ucs < 32 || ucs == 0x7F) ? 0 : 1;
    }
    
    // Direct lookup for first 4K code points
    if (ucs < DIRECT_WIDTH_LOOKUP.length) {
        return DIRECT_WIDTH_LOOKUP[ucs];
    }
    
    // Segmented lookup for BMP
    if (ucs < 0x10000) {
        int segment = (ucs >>> 8) & 0xFF;
        if (BMP_SEGMENT_LOOKUP[segment] != null) {
            return BMP_SEGMENT_LOOKUP[segment][ucs & 0xFF];
        }
    }
    
    // Fallback to binary search for rare cases
    return widthSlowPath(ucs);
}
```

**Expected Impact:** 3-5x speedup for width calculations, 15-25% overall rendering improvement

---

## 2. TerminalRow Array Allocation Optimization

### Current Issue
In `TerminalRow.setChar()`, array growth happens frequently when dealing with combining characters and wide characters:
```java
if (mSpaceUsed + javaCharDifference > text.length) {
    char[] newText = new char[text.length + mColumns];
    System.arraycopy(...);
    mText = text = newText;
}
```

### Recommendation: Pre-allocate with Better Growth Strategy

**Priority: MEDIUM**

1. **Increase initial spare capacity factor** from 1.5f to 2.0f to reduce reallocations
2. **Use geometric growth** (double size) instead of linear growth when expanding
3. **Implement array pooling** for frequently allocated rows

```java
private static final float SPARE_CAPACITY_FACTOR = 2.0f; // Increased from 1.5f

// When growing array:
int newSize = Math.max(text.length * 2, text.length + mColumns);
char[] newText = new char[newSize];
```

**Expected Impact:** 20-30% reduction in GC pressure, smoother scrolling

---

## 3. TerminalRenderer Text Measurement Caching

### Current Issue
In `TerminalRenderer.render()`, text measurement is performed repeatedly:
```java
final float measuredCodePointWidth = (codePoint < asciiMeasures.length) 
    ? asciiMeasures[codePoint] 
    : mTextPaint.measureText(line, currentCharIndex, charsForCodePoint);
```

The `mTextPaint.measureText()` call for non-ASCII characters is expensive and called for every unique character.

### Recommendation: Implement LRU Cache for Character Widths

**Priority: MEDIUM**

```java
private final float[] asciiMeasures = new float[127];
private final FloatIntLruCache nonAsciiWidthCache = new FloatIntLruCache(512);

private float measureCodePoint(char[] text, int index, int charsForCodePoint, int codePoint) {
    if (codePoint < asciiMeasures.length) {
        return asciiMeasures[codePoint];
    }
    
    Float cached = nonAsciiWidthCache.get(codePoint);
    if (cached != null) {
        return cached;
    }
    
    float width = mTextPaint.measureText(text, index, charsForCodePoint);
    nonAsciiWidthCache.put(codePoint, width);
    return width;
}
```

**Expected Impact:** 10-20% rendering speedup for text with repeated non-ASCII characters

---

## 4. TerminalBuffer Resize Operation Optimization

### Current Issue
The `resize()` method in `TerminalBuffer` performs extensive copying and reallocation:
```java
mLines = new TerminalRow[newTotalRows];
for (int i = 0; i < newTotalRows; i++)
    mLines[i] = new TerminalRow(newColumns, currentStyle);
```

### Recommendation: Lazy Row Allocation and Reuse

**Priority: LOW-MEDIUM**

1. **Reuse existing rows** when columns don't change significantly
2. **Lazy allocation** - only allocate rows when they're actually written to
3. **Row pooling** to avoid frequent allocations during resize operations

```java
// Add row pool
private static final ArrayDeque<TerminalRow> sRowPool = new ArrayDeque<>(100);

private TerminalRow obtainRow(int columns, long style) {
    TerminalRow row = sRowPool.pollFirst();
    if (row != null && row.mColumns == columns) {
        row.clear(style);
        return row;
    }
    return (row != null) ? new TerminalRow(columns, style) : row;
}

private void recycleRow(TerminalRow row) {
    if (sRowPool.size() < 100) {
        sRowPool.offerFirst(row);
    }
}
```

**Expected Impact:** Reduced memory churn during terminal resize operations

---

## 5. Canvas Drawing Batching

### Current Issue
In `TerminalRenderer.drawTextRun()`, multiple canvas operations are performed per text run:
- Background rectangle drawing
- Cursor drawing  
- Text drawing
- Matrix save/restore operations

### Recommendation: Batch Similar Operations

**Priority: MEDIUM**

1. **Batch background rectangles** - combine adjacent cells with same background color
2. **Defer cursor drawing** - draw all cursors in single pass at end
3. **Minimize matrix operations** - track state changes more efficiently

```java
// Track consecutive runs with same background
class BackgroundBatch {
    float left, right;
    int color;
    int count;
    
    void add(float cellLeft, float cellRight, int cellColor) {
        if (cellColor == color && cellLeft <= right) {
            right = cellRight;
            count++;
        } else {
            flush();
            left = cellLeft;
            right = cellRight;
            color = cellColor;
            count = 1;
        }
    }
    
    void flush() {
        if (count > 0) {
            mTextPaint.setColor(color);
            canvas.drawRect(left, top, right, bottom, mTextPaint);
        }
    }
}
```

**Expected Impact:** 15-25% reduction in canvas operations, smoother rendering

---

## 6. Avoid Redundant Character.isHighSurrogate Calls

### Current Issue
Multiple places check `Character.isHighSurrogate()` redundantly:
- `TerminalRow.findStartOfColumn()` checks multiple times
- `TerminalRenderer.render()` checks in tight loop

### Recommendation: Cache Surrogate Information

**Priority: LOW**

Since surrogate status doesn't change, encode it alongside character data or use bit manipulation tricks:

```java
// Quick check: high surrogates are in range 0xD800-0xDBFF
// Can use bitwise operations for faster check:
static boolean isHighSurrogateFast(char c) {
    return (c & 0xFC00) == 0xD800;
}
```

**Expected Impact:** 5-10% speedup in character iteration loops

---

## 7. TerminalView Input Event Coalescing

### Current Issue
Rapid touch events may cause excessive invalidation and redraws.

### Recommendation: Implement Event Coalescing

**Priority: MEDIUM**

```java
private Runnable mInvalidateRunnable;
private long mLastInvalidationTime;
private static final long MIN_INVALIDATION_INTERVAL_MS = 8; // ~120 FPS cap

@Override
public boolean onTouchEvent(MotionEvent e) {
    // Coalesce rapid touch events
    long currentTime = System.currentTimeMillis();
    if (currentTime - mLastInvalidationTime < MIN_INVALIDATION_INTERVAL_MS) {
        // Remove pending invalidate if too soon
        removeCallbacks(mInvalidateRunnable);
    }
    
    mLastInvalidationTime = currentTime;
    postDelayed(mInvalidateRunnable, MIN_INVALIDATION_INTERVAL_MS);
    return true;
}
```

**Expected Impact:** Smoother touch interaction, reduced power consumption

---

## 8. Use StringBuilder More Efficiently

### Current Issue
In `TerminalBuffer.getSelectedText()`, StringBuilder may reallocate multiple times:
```java
final StringBuilder builder = new StringBuilder();
```

### Recommendation: Pre-size StringBuilder

**Priority: LOW**

```java
// Estimate size needed based on selection area
int estimatedChars = (selY2 - selY1 + 1) * mColumns;
final StringBuilder builder = new StringBuilder(estimatedChars);
```

**Expected Impact:** Minor reduction in copy operations for large selections

---

## Implementation Priority

### Phase 1 (High Impact, Low Risk)
1. WcWidth lookup table optimization
2. TerminalRow spare capacity increase
3. Character.isHighSurrogate fast path

### Phase 2 (Medium Impact, Medium Complexity)
4. TerminalRenderer measurement caching
5. Canvas drawing batching
6. Input event coalescing

### Phase 3 (Lower Impact, Higher Complexity)
7. TerminalBuffer lazy allocation
8. Row pooling system
9. StringBuilder pre-sizing

---

## Testing Recommendations

Before implementing optimizations:
1. **Establish baseline metrics**: Frame rendering time, GC frequency, memory usage
2. **Create performance test suite**: Automated tests for common operations
3. **Profile real-world usage**: Record typical terminal sessions
4. **A/B testing**: Compare optimized vs. original versions

Tools to use:
- Android Profiler for CPU/memory profiling
- Systrace for frame rendering analysis
- Custom benchmarking harness for micro-benchmarks

---

## Conclusion

These optimizations target the most performance-critical paths in the terminal emulator:
- Character width calculation (hot path in rendering)
- Memory allocation patterns (GC pressure reduction)
- Canvas operations (rendering efficiency)
- Event handling (user experience)

Expected overall improvement: **30-50% reduction in CPU usage** during typical terminal usage, with **smoother scrolling and text rendering**.
