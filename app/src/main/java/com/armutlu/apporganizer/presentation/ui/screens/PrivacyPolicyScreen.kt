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

private const val PP_WEB_URL = "https://hekizoglu.github.io/android-folderautomanager/privacy_policy.html"

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
                body = "Son güncelleme: 10 Temmuz 2026\n\nBu gizlilik politikası, AppOrganizer uygulamasının eriştiği verileri nasıl kullandığını, sakladığını ve koruduğunu açıklar."
            )
            PolicySection(
                title = "Toplanan Veriler",
                body = "• Yüklü uygulama listesi: Tüm uygulamaları listelemek, aramak, kategorilere ayırmak ve başlatmak için okunur. Paket bazlı envanter cihazda kalır; reklam veya analitik sağlayıcılarına gönderilmez.\n\n• Kullanım istatistikleri: Hangi uygulamaları ne sıklıkta kullandığınız (UsageStats API). Öneriler ve haftalık raporlar için cihazda işlenir.\n\n• Bildirim erişimi: Rozet için sayı saklanır. Bildirim Metni özelliğini açarsanız son bildirimin kısa metni yalnızca cihazda saklanıp gösterilir.\n\n• İsteğe bağlı kişiler ve dosya dizini: Arama kaynağını açarsanız yalnız cihazda indekslenir; kaynak kapatılınca indeks temizlenir.\n\n• Ayarlar ve tercihler: Uygulamanın yerel veritabanında ve SharedPreferences dosyalarında saklanır."
            )
            PolicySection(
                title = "Toplanmayan Veriler",
                body = "• Kişisel kimlik bilgisi (ad, e-posta, telefon numarası)\n• Konum verisi\n• Kamera veya mikrofon erişimi\n• Fotoğraf veya dosya içerikleri\n• Reklam tanımlayıcıları"
            )
            PolicySection(
                title = "Üçüncü Taraflar",
                body = "AppOrganizer verilerinizi satmaz ve reklam ağlarıyla paylaşmaz.\n\nFirebase Analytics özellik/ekran kullanım olaylarını; Firebase Crashlytics teknik çökme kayıtlarını Google'a gönderebilir. Yüklü veya açılan uygulamaların paket adları bu analitik olaylarına eklenmez.\n\nİsteğe bağlı DeepSeek entegrasyonu etkinleştirilirse bilinmeyen uygulamanın adı ve paket adı kategorileme amacıyla DeepSeek'e gönderilir."
            )
            PolicySection(
                title = "Veri Güvenliği",
                body = "Yerel uygulama envanteri, kullanım geçmişi, bildirim verileri ve arama indeksleri cihazda saklanır. Firebase ve isteğe bağlı DeepSeek aktarımı HTTPS üzerinden yapılır. Yedek dosyası yalnız sizin seçtiğiniz Android belge sağlayıcısına yazılır; Google Drive gibi bir bulut sağlayıcısı seçerseniz sağlayıcının kendi saklama ve gizlilik koşulları geçerlidir."
            )
            PolicySection(
                title = "Saklama Süresi",
                body = "Yerel veriler özellik çalıştığı veya siz silene kadar tutulur. Bildirim analiz olayları 30 günden eski olduğunda temizlenir. Kişi/dosya arama kaynağı kapatıldığında ilgili indeks silinir. Firebase ve DeepSeek tarafındaki saklama süreleri ilgili sağlayıcının politikasına tabidir."
            )
            PolicySection(
                title = "Veri Silme",
                body = "Yerel kullanım ve kişiselleştirme verilerini Ayarlar → Hakkında & Yedekleme → Tüm Verileri Sıfırla ile temizleyebilirsiniz. Tam silme için Android Ayarlar → Uygulamalar → AppOrganizer → Depolamayı Temizle seçeneğini kullanın. Uygulama kaldırıldığında yerel veriler silinir; dışa aktardığınız yedekler seçtiğiniz konumda kalır ve ayrıca silinmelidir. Firebase/DeepSeek hakkında silme talebi veya gizlilik sorusu için aşağıdaki iletişim adresini kullanabilirsiniz."
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
