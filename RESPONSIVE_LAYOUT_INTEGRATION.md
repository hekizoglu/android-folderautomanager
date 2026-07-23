# Responsive Layout Integration Plan — StandardLayoutContainer Application

**Amaç:** StandardLayoutContainer composable'ı HomeScreen + AllAppsDrawer + FolderScreen + SettingsScreen'e entegre etme.  
**Faz:** R-HOME-LAYOUT backlog  
**Ölçek:** 4 ekran × 2 değişiklik (container wrap + grid update) = 8 integration point

---

## 1. HomeScreen Integration

### Geçerli Yapı (status quo)
```kotlin
@Composable
fun HomeScreen(...) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), // sabit
    ) {
        // klasör grid
    }
}
```

### Hedefli Yapı
```kotlin
@Composable
fun HomeScreen(...) {
    StandardLayoutContainer { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding, // responsive
        ) {
            // klasör grid, getResponsiveGridColumns() ile column count
        }
    }
}
```

**Adımlar:**
1. [ ] HomeScreen.kt'de StandardLayoutContainer import
2. [ ] Column/LazyColumn'u container içine wrap
3. [ ] contentPadding prop'unu responsive padding'e bağla
4. [ ] getResponsiveGridColumns() ile folder grid column count'u set et
5. [ ] Compile + visual test (emulator)

---

## 2. AllAppsDrawer Integration

### Geçerli
```kotlin
@Composable
fun AllAppsDrawer(...) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp), // sabit
    ) {
        // app list
    }
}
```

### Hedefli
```kotlin
@Composable
fun AllAppsDrawer(...) {
    StandardLayoutContainer { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            // app list
        }
    }
}
```

**Adımlar:**
1. [ ] AllAppsDrawer.kt'de StandardLayoutContainer import
2. [ ] LazyColumn'u container wrap
3. [ ] contentPadding responsive
4. [ ] Compile + visual test

---

## 3. FolderScreen Integration

### Geçerli
```kotlin
@Composable
fun FolderScreen(...) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp), // sabit
    ) {
        // folder apps
    }
}
```

### Hedefli
```kotlin
@Composable
fun FolderScreen(...) {
    StandardLayoutContainer { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding, // responsive
        ) {
            // folder apps, grid column adaptive
        }
    }
}
```

**Adımlar:**
1. [ ] FolderScreen.kt'de StandardLayoutContainer import
2. [ ] LazyColumn wrap
3. [ ] contentPadding responsive
4. [ ] getResponsiveGridColumns() ile app grid column
5. [ ] Compile + visual test

---

## 4. SettingsScreen Integration

### Geçerli
```kotlin
@Composable
fun SettingsScreen(...) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        // settings sections
    }
}
```

### Hedefli
```kotlin
@Composable
fun SettingsScreen(...) {
    StandardLayoutContainer { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            // settings sections
        }
    }
}
```

**Adımlar:**
1. [ ] SettingsScreen.kt'de StandardLayoutContainer import
2. [ ] LazyColumn wrap
3. [ ] contentPadding responsive
4. [ ] Compile + visual test

---

## Testing Checklist

### Unit/Compile
- [ ] compileDebugKotlin ✅ (4 files)
- [ ] testDebugUnitTest ✅

### Visual (Emulator)
- [ ] **Phone (<600dp):** 16dp padding, 4-col grid
- [ ] **Tablet (600–800dp):** 24dp padding, 5-col grid
- [ ] **Large (800+dp):** 32dp padding, 6-col grid
- [ ] No overflow, no cut-off text
- [ ] Rotation: padding/grid persist

### Regression
- [ ] Dokun navigation: henüz hata yok
- [ ] Scroll: smooth
- [ ] Keyboard: dialogs render

---

## Commit Strategy

**Madde başına 1 screen:**
1. `HomeScreen StandardLayoutContainer wrap` → commit
2. `AllAppsDrawer StandardLayoutContainer wrap` → commit
3. `FolderScreen StandardLayoutContainer wrap` → commit
4. `SettingsScreen StandardLayoutContainer wrap` → commit

**veya toplu:** 4 screen + test + 1 commit (recommended if compile < 1m)

---

## Timeline

- [ ] Madde 1: HomeScreen integration — ~15 min
- [ ] Madde 2: AllAppsDrawer integration — ~10 min
- [ ] Madde 3: FolderScreen integration — ~10 min
- [ ] Madde 4: SettingsScreen integration — ~10 min
- [ ] Visual test: ~5 min
- [ ] **Total:** ~50 min (single CRON cycle)

---

**Next Steps:** ROADMAP 4 maddeyi [ ]'den [x]'e döndüğünde (visual test pass ile), %70 hedefi ~%60 civarına ulaşır (15+ madde kaldı → CRON-57 final push).
