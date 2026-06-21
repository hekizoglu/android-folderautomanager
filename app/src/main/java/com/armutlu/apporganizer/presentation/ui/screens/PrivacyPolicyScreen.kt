package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PP_WEB_URL = "https://hekizoglu.github.io/android-folderautomanager/docs/privacy_policy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gizlilik Politikası") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PP_WEB_URL)))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Tarayıcıda aç")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicySection(
                title = "AppOrganizer Gizlilik Politikası",
                body = "Son güncelleme: 21 Haziran 2026\n\nBu gizlilik politikası, AppOrganizer uygulamasının kişisel verilerinizi nasıl topladığını, kullandığını ve koruduğunu açıklar."
            )
            PolicySection(
                title = "Toplanan Veriler",
                body = "• Yüklü uygulama listesi: Uygulamaları kategorilere ayırmak için cihazınızdaki uygulamalar okunur. Bu veriler yalnızca cihazınızda saklanır, hiçbir sunucuya gönderilmez.\n\n• Kullanım istatistikleri: Hangi uygulamaları ne sıklıkta kullandığınız (UsageStats API). Yalnızca ana ekranda öneriler için kullanılır, cihaz dışına çıkmaz.\n\n• Bildirim erişimi: Bildirim sayılarını (badge) görüntülemek için. Bildirim içerikleri saklanmaz.\n\n• Ayarlar ve tercihler: Cihazınızdaki SharedPreferences'te saklanır."
            )
            PolicySection(
                title = "Toplanmayan Veriler",
                body = "• Kişisel kimlik bilgisi (ad, e-posta, telefon numarası)\n• Konum verisi\n• Kamera veya mikrofon erişimi\n• Fotoğraf veya dosya içerikleri\n• Reklam tanımlayıcıları"
            )
            PolicySection(
                title = "Üçüncü Taraflar",
                body = "AppOrganizer, kullanıcı verilerini hiçbir üçüncü tarafla paylaşmaz, satmaz veya kiralamaz.\n\nİsteğe bağlı olarak etkinleştirilen DeepSeek API entegrasyonu (bilinmeyen uygulamaları kategorize etmek için), yalnızca uygulama adı ve paket adını anonim olarak gönderir. Kişisel veri içermez."
            )
            PolicySection(
                title = "Veri Güvenliği",
                body = "Tüm veriler cihazınızda yerel olarak saklanır. Uygulama internet bağlantısı yalnızca isteğe bağlı DeepSeek kategorize özelliği için kullanılır ve HTTPS ile şifrelenir."
            )
            PolicySection(
                title = "Veri Silme",
                body = "Tüm uygulama verilerini silmek için: Ayarlar → Uygulamalar → AppOrganizer → Depolamayı Temizle seçeneğini kullanabilirsiniz. Uygulama kaldırıldığında tüm veriler otomatik olarak silinir."
            )
            PolicySection(
                title = "İletişim",
                body = "Gizlilik ile ilgili sorularınız için: huseyinekizoglu@gmail.com"
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Web'de görüntüle: $PP_WEB_URL",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(body, fontSize = 13.sp, lineHeight = 20.sp)
        HorizontalDivider()
    }
}
