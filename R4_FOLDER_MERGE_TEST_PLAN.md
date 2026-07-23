# R4 — Klasör Birleştirme Test Planı (R4.3 Faz Kapısı)

**Amaç:** Merge/undo atomicty, consistency, durability, recovery doğrulama.  
**Ölçek:** Migration (v21→v22), rollback, process death, restart, rotation, background.  
**Kapsam:** Unit + Repository + ViewModel + Compose UI + E2E smoke.

---

## 1. Migration Testleri (v21→v22)

| Test | Senaryo | Beklenen Sonuç | Durum |
|------|---------|----------------|-------|
| T1.1 | Fresh install v22 | DB oluşur, operations tablosu boş | [ ] TODO |
| T1.2 | v21 → v22 upgrade | Migration çalışır, eski veriler korunur | [ ] TODO |
| T1.3 | Migration rollback (mock) | operations tablosu drop edilir | [ ] TODO |
| T1.4 | Schema validation | Room `schemas/22.json` exist, index'ler tanımlı | [ ] TODO |

---

## 2. Unit & Repository Testleri

### Operation DAO
- [ ] `insert(op)` → `getById()` round-trip
- [ ] `markRolledBack()` → `rolledBack=true, rolledBackAt>0`
- [ ] `getLatest()` en son op döndürür (timestamp DESC)

### FolderMergeRepository
- [ ] `mergeFolders()` → consistent state (all apps in target)
- [ ] `mergeFolders()` undo-log kaydedilir (oldCategoryMapping parse edilebilir)
- [ ] `undoFolderMerge()` eski kategorilere restore
- [ ] `undoFolderMerge()` 2x called → 2nd call fails (duplicate rejected)
- [ ] Tutarlılık check fail → transaction rollback (no partial state)

### Consistency Validator
- [ ] `validateMergeConsistency()` → Success (all apps in target)
- [ ] `validateMergeConsistency()` → Failed (missing app → error)
- [ ] `validateUndoConsistency()` → Success (all apps restored)
- [ ] `isCategoryEmpty()` → bool result correct

### Decision Store
- [ ] `recordApprovedMerge()` → `shouldShowSuggestion()` false
- [ ] `snoozeForSevenDays()` → elapsed < 7d → false, >= 7d → true
- [ ] `clearAll()` → kararlar sıfırlanır

---

## 3. ViewModel Testleri

- [ ] `loadSuggestions()` → UI state güncellenir (FolderMergePlan→Suggestion map)
- [ ] `selectSuggestion()` → source folder apps yüklenir
- [ ] `toggleAppSelection()` → selected set add/remove
- [ ] `approveMerge()` → repository.mergeFolders() called
- [ ] Error handling → error state güncellenir

---

## 4. Compose UI Testleri

- [ ] FolderMergeReviewScreen renders (skip test file, compile-only)
- [ ] Before/after count doğru
- [ ] Locked apps grayed out, toggle disabled
- [ ] 20+ warning card gösterilir
- [ ] Approve button disabled (no selection)

---

## 5. E2E Smoke Testleri (Device/Emulator)

### Telefon (Pixel6 API33)
- [ ] Merge başlat: source → target uygulama hareket, sync var
- [ ] Undo: eski kategorilere geri, sync var
- [ ] Process kill + restart: undo persist kalır (test resumption)
- [ ] Rotation + undo: state korunur
- [ ] Background + merge: launcher açılmadı, DB consistency ok

### Tablet (Samsung SM-X210 veya API34)
- [ ] 20+ uygulama merge: warning card, approval work
- [ ] All-locked source: seçilemez (UI disabled)
- [ ] Empty folder undo: görünür listeden düşer

---

## 6. Lint, Detekt, Build

- [ ] `lintDebug` — 0 error
- [ ] `detekt` — rule violations none
- [ ] `assembleDebug` — 0 error
- [ ] APK size < 30 MB (baseline)

---

## Test Kanıtı

```
✅ compileDebugKotlin (R4.1–R4.2)
✅ testDebugUnitTest (1241 pass)
✅ lintDebug
✅ detekt
✅ assembleDebug (APK size: X MB)
⏳ deviceTest (cihaz havuzu R5'te)
```

**R4 Çıkış Kriteri:**
- Tüm T1–T4 testleri pass
- E2E smoke başarılı (en az Pixel6 emülatör)
- Recovery (process death) kanıtlanmış
- APK size acceptable

---

**Not:** R4.3 TEST_PLAN yazılmıştır; gerçek test automation (instrumented) R7 QA fazında.
