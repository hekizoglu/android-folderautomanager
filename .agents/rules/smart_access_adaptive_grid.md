# Smart Access Grid Layout & Centering Rule

## Overview
Akıllı Erişim (Smart Access) kartında ve grid dizilimlerinde, mobil ve tablet gibi farklı ekran genişliklerine göre dinamik ve hizalı yerleşim yapılmalıdır.

## Specifications & Rules
1. **Minimum Column Count**: Ekran genişliğine bakılmaksızın her satıra **en az 4 adet** öge sığmalıdır (`columns = maxOf(4, ...)`).
2. **Tablet / Large Screen Adaptivity**: Geniş ekranlarda (örn. 600dp+, 840dp+ tablet veya katlanabilir cihazlar) öge genişliğine göre sütun sayısı dinamik hesaplanmalıdır.
3. **Horizontal Centering**: 8'li, 16'lı veya herhangi bir öge sayısında satır içi ögeler her zaman yatayda ortalanmalıdır (`Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)`).
4. **Official Guidelines**: Jetpack Compose düzen tasarımlarında `developer.android.com` kılavuzlarındaki `FlowRow` ve `Adaptive Layout` prensiplerine uygun olarak responsive hesaplamalar yapılmalıdır.
