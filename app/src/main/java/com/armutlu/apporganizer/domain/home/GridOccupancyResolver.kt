package com.armutlu.apporganizer.domain.home

/**
 * Faz S (Serbest Sürükle-Bırak Ana Ekran Sistemi) — S1 saf domain katmanı. Grid hücre
 * matematiği: dolu/boş hücre hesaplama, çakışma kontrolü, ilk uygun boş konum arama.
 *
 * SAF — Context, Room, Compose hiçbir bağımlılık YOK. Tamamen test edilebilir matematik.
 * Şu an hiçbir UI/ViewModel bu dosyayı çağırmıyor.
 */

/** Grid üzerinde bir dikdörtgenin konumu ve kapladığı hücre sayısı (span). */
data class GridPosition(
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
)

/** Grid'in toplam sütun/satır sayısı. */
data class GridBounds(
    val columns: Int,
    val rows: Int,
)

/**
 * İki dikdörtgenin (cellX, cellY, spanX, spanY) çakışıp çakışmadığını AABB (axis-aligned
 * bounding box) mantığıyla hesaplar.
 */
fun hasOverlap(a: GridPosition, b: GridPosition): Boolean {
    val aRight = a.cellX + a.spanX
    val aBottom = a.cellY + a.spanY
    val bRight = b.cellX + b.spanX
    val bBottom = b.cellY + b.spanY

    val separatedHorizontally = aRight <= b.cellX || bRight <= a.cellX
    val separatedVertically = aBottom <= b.cellY || bBottom <= a.cellY

    return !(separatedHorizontally || separatedVertically)
}

/**
 * [candidate] grid sınırları içinde mi VE mevcut [occupied] listesindeki hiçbir öğeyle
 * çakışmıyor mu.
 */
fun isValidPlacement(candidate: GridPosition, occupied: List<GridPosition>, bounds: GridBounds): Boolean {
    val withinBounds = candidate.cellX >= 0 &&
        candidate.cellY >= 0 &&
        candidate.cellX + candidate.spanX <= bounds.columns &&
        candidate.cellY + candidate.spanY <= bounds.rows

    if (!withinBounds) return false

    return occupied.none { hasOverlap(candidate, it) }
}

/**
 * Verilen dolu hücreler ve span'e göre soldan-sağa yukarıdan-aşağıya ilk boş uygun hücreyi
 * bulur (satır satır tarar, spanX/spanY kadar dikdörtgenin tamamen boş ve sınırlar içinde
 * olduğu ilk konumu döner, yoksa null).
 */
fun findFirstFreeCell(
    occupied: List<GridPosition>,
    bounds: GridBounds,
    spanX: Int = 1,
    spanY: Int = 1,
): GridPosition? {
    for (y in 0 until bounds.rows) {
        for (x in 0 until bounds.columns) {
            val candidate = GridPosition(cellX = x, cellY = y, spanX = spanX, spanY = spanY)
            if (isValidPlacement(candidate, occupied, bounds)) {
                return candidate
            }
        }
    }
    return null
}
