
package gas.hostinggo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import android.view.*
import java.io.*
import fi.iki.elonen.NanoHTTPD
import androidx.core.content.FileProvider
import android.net.Uri
import android.content.*
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.Glide
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class MainActivity : AppCompatActivity() {

    private var currentPath: File? = null
    private var fileClipboard: File? = null
    private var isCut: Boolean = false
    private val REQUEST_FILE = 100
    private val REQUEST_FOLDER = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        JalurSekarang(filesDir)

        findViewById<ImageView>(R.id.nav).setOnClickListener {
            val liner = findViewById<LinearLayout>(R.id.liner)
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        val memoApk = File(filesDir, "memogo")
        if (!memoApk.exists()) {
            memoApk.mkdirs()
        }
        SiapkanGrid(filesDir)
        BuatBaru()
        
        findViewById<ImageView>(R.id.tfFile).setOnClickListener{
        TransferFile(true)}
        
        findViewById<ImageView>(R.id.tfFol).setOnClickListener{
        TransferFile(false)}
        
        findViewById<LinearLayout>(R.id.uji).setOnClickListener{ UjiPing()}
        
        val mulaiHost = findViewById<Switch>(R.id.mulaiHost)
        mulaiHost.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) { MulaiHost() }
        else { StopHost() }
        }
        //kode
    }

    private fun SiapkanGrid(folder: File) {
    val grid = findViewById<GridLayout>(R.id.grid)
    grid.removeAllViews()

    folder.listFiles()?.forEach { file ->
        val itemGrid = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
        val gambar = itemGrid.findViewById<ImageView>(R.id.gambar)
        val nama = itemGrid.findViewById<TextView>(R.id.nama)

        nama.text = file.name

        val ikon = when (file.extension.lowercase()) {
            "mp4", "avi", "mkv", "webm" -> R.drawable.video
            "mp3", "wav", "ogg" -> R.drawable.musik
            "kt" -> R.drawable.kt
            "java" -> R.drawable.java
            "js" -> R.drawable.js
            "c", "h" -> R.drawable.c
            "py" -> R.drawable.py
            "pdf" -> R.drawable.pdf
            "txt" -> R.drawable.txt
            "doc", "docx" -> R.drawable.doc
            "xml" -> R.drawable.xml
            "json" -> R.drawable.json
            "zip", "rar", "7z", "tar", "gz" -> R.drawable.kompres
            "apk" -> R.drawable.apk
            "gradle", "kts" -> R.drawable.gradle
            "exe" -> R.drawable.exe
            "sh", "bin" -> R.drawable.sh
            else -> R.drawable.file
        }

        if (file.extension.lowercase() in listOf("png", "jpg", "jpeg", "gif", "bmp")) {
            Glide.with(this)
                .load(file)
                .into(gambar)
        } else {
            gambar.setImageResource(if (file.isDirectory) R.drawable.folder else ikon)
        }

        grid.addView(itemGrid)

        itemGrid.setOnClickListener {
            if (file.isDirectory) BukaFolder(file)
            else BukaFile(file)
        }

        itemGrid.setOnLongClickListener {
            TekanLama(file)
            true
        }
    }
}

    private fun JalurSekarang(path: File) {
        val jalur = findViewById<TextView>(R.id.jalur)
        currentPath = path
        jalur.text = path.absolutePath
    }

    private fun Segarkan() {
        val grid = findViewById<GridLayout>(R.id.grid)
        grid.removeAllViews()
        SiapkanGrid(currentPath ?: filesDir)
    }

    private fun BukaFolder(folder: File) {
    val kembali = findViewById<ImageView>(R.id.kembali)

    if (folder.isDirectory) {
        JalurSekarang(folder)
        Segarkan()
        kembali.visibility = if (folder.absolutePath == filesDir.absolutePath) View.GONE else View.VISIBLE
    }

    kembali.setOnClickListener {
        val parent = currentPath?.parentFile ?: return@setOnClickListener
        if (parent.absolutePath == filesDir.absolutePath) {
            kembali.visibility = View.GONE
        }
        BukaFolder(parent)
        }
    }
    
    private fun TekanLama(file: File): Boolean {
        val Tlama = findViewById<LinearLayout>(R.id.lama)
        Tlama.visibility = View.VISIBLE 

        val menempel = findViewById<ImageView>(R.id.tempel)
        menempel.visibility = View.GONE 

        findViewById<TextView>(R.id.tutupLama).setOnClickListener {
        Tlama.visibility = View.GONE }

        findViewById<ImageView>(R.id.salin).setOnClickListener {
            fileClipboard = file
            isCut = false
            menempel.visibility = View.VISIBLE
        }

        findViewById<ImageView>(R.id.potong).setOnClickListener {
            fileClipboard = file
            isCut = true
            menempel.visibility = View.VISIBLE
        }

        findViewById<ImageView>(R.id.hapus).setOnClickListener {
            if (file.exists()) {
                if (file.isDirectory) { file.deleteRecursively()
                } else { file.delete() }
            }
            Tlama.visibility = View.GONE 
            Segarkan() 
        }

        findViewById<ImageView>(R.id.rename).setOnClickListener {
            Namai(file)
            Tlama.visibility = View.GONE
        }

        findViewById<ImageView>(R.id.tempel).setOnClickListener {
            Tempel(currentPath ?: filesDir)
        }

        return true 
    }
    
    private fun Tempel(folder: File) {
    val menempel = findViewById<ImageView>(R.id.tempel)

    fileClipboard?.let { file ->
        val targetFile = File(folder, file.name)

        if (file.exists()) {
            if (file.isDirectory) { file.copyRecursively(targetFile) 
            } else { file.copyTo(targetFile, overwrite = true) }

            if (isCut) { file.deleteRecursively() }
        }
    }

    fileClipboard = null
    isCut = false
    menempel.visibility = View.GONE

    Segarkan() 
    }
    
    private fun Namai(file: File) {
    val dialog = AlertDialog.Builder(this)
    val input = EditText(this).apply {
        hint = "Tulis nama baru"
        setText(file.name)
    }

    dialog.setView(input)
    dialog.setPositiveButton("Simpan") { _, _ ->
        val namaBaru = input.text.toString().trim()
        if (namaBaru.isNotEmpty()) {
            val fileBaru = File(file.parent, namaBaru)
            if (!fileBaru.exists()) {
                file.renameTo(fileBaru)
                Segarkan() 
            } else {
                Toast.makeText(this, "Nama sudah ada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

        dialog.setNegativeButton("Batal", null)
        dialog.show()
    }
    
    private fun BuatBaru() {
        findViewById<ImageView>(R.id.fileBaru).setOnClickListener { BuatFile() }

        findViewById<ImageView>(R.id.folderBaru).setOnClickListener { BuatFolder() }
    }
    
    private fun BuatFile() {
    val dialog = AlertDialog.Builder(this)
    val input = EditText(this).apply { hint = "Nama file" }

    dialog.setView(input)
    dialog.setPositiveButton("Buat") { _, _ ->
        val namaFile = input.text.toString().trim()
        if (namaFile.isNotEmpty()) {
            val fileBaru = File(currentPath ?: filesDir, namaFile)
            if (!fileBaru.exists()) {
                fileBaru.createNewFile()
                Segarkan()
            } else {
                Toast.makeText(this, "File sudah ada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    dialog.setNegativeButton("Batal", null)
    dialog.show()
}

    private fun BuatFolder() {
    val dialog = AlertDialog.Builder(this)
    val input = EditText(this).apply { hint = "Nama folder" }

    dialog.setView(input)
    dialog.setPositiveButton("Buat") { _, _ ->
        val namaFolder = input.text.toString().trim()
        if (namaFolder.isNotEmpty()) {
            val folderBaru = File(currentPath ?: filesDir, namaFolder)
            if (!folderBaru.exists()) {
                folderBaru.mkdirs()
                Segarkan()
            } else {
                Toast.makeText(this, "Folder sudah ada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

        dialog.setNegativeButton("Batal", null)
        dialog.show()
    }
    

    private fun TransferFile(isFile: Boolean) {
    val intent = if (isFile) {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    } else {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }
    startActivityForResult(intent, if (isFile) REQUEST_FILE else REQUEST_FOLDER)
}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == RESULT_OK) {
        val uri = data?.data ?: return
        if (requestCode == REQUEST_FILE) {
            SalinFile(uri)
        } else if (requestCode == REQUEST_FOLDER) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            SalinFolder(uri)
        }
    }
}
    
    private fun SalinFile(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val fileNama = uri.lastPathSegment?.substringAfterLast("/")
        val fileTujuan = File(currentPath ?: filesDir, fileNama ?: "file_baru")

    inputStream?.use { input ->
        fileTujuan.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    Segarkan()
        Toast.makeText(this, "File berhasil disalin!", Toast.LENGTH_SHORT).show()
    }
    
    private fun SalinFolder(folderUri: Uri) {
    val folderDoc = DocumentFile.fromTreeUri(this, folderUri) ?: return
    val tujuan = File(filesDir, folderDoc.name ?: "FolderBaru")
    if (!tujuan.exists()) tujuan.mkdirs()

    for (file in folderDoc.listFiles()) {
        if (file.isFile) {
            val inputStream = contentResolver.openInputStream(file.uri) ?: continue
            val outputFile = File(tujuan, file.name ?: "TanpaNama")
            val outputStream = FileOutputStream(outputFile)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        }
    }
    Segarkan()
    Toast.makeText(this, "Folder berhasil disalin: ${tujuan.absolutePath}", Toast.LENGTH_SHORT).show()
    }

private fun getMimeType(ext: String): String {
    return when (ext.lowercase()) {
        "mp3", "wav", "ogg" -> "audio/*"
        "mp4", "avi", "mkv", "webm" -> "video/*"
        "jpg", "jpeg", "png", "gif", "bmp" -> "image/*"
        "pdf" -> "application/pdf"
        "txt", "java", "kt", "xml", "json", "py", "c", "h", "sh" -> "text/plain"
        "zip", "rar", "7z", "tar", "gz" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
        }
    }
    
    private fun UjiPing() {
    val up = findViewById<TextView>(R.id.up)
    val down = findViewById<TextView>(R.id.down)
    val linknya = findViewById<EditText>(R.id.linknya)

    val url = linknya.text.toString().trim()
    if (url.isEmpty()) {
        Toast.makeText(this, "MASUKKAN LINKNYA DULU", Toast.LENGTH_SHORT).show()
        return
    }

    Thread {
        try {
            val koneksi = URL(url).openConnection() as HttpURLConnection
            koneksi.connect()
            val inputStream = koneksi.inputStream
            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }

            val endTime = System.currentTimeMillis()
            val downloadSpeed = totalBytes / ((endTime - startTime) / 1000.0) 

            inputStream.close()
            koneksi.disconnect()

            val uploadData = ByteArray(1024 * 1024) 
            val uploadStartTime = System.currentTimeMillis()

            val uploadKoneksi = URL(url).openConnection() as HttpURLConnection
            uploadKoneksi.requestMethod = "POST"
            uploadKoneksi.doOutput = true
            uploadKoneksi.setRequestProperty("Content-Type", "application/octet-stream")

            val outputStream = uploadKoneksi.outputStream
            outputStream.write(uploadData)
            outputStream.flush()
            outputStream.close()

            val uploadEndTime = System.currentTimeMillis()
            val uploadSpeed = uploadData.size / ((uploadEndTime - uploadStartTime) / 1000.0)

            runOnUiThread {
                down.text = "Download: %.2f KB/s".format(downloadSpeed / 1024)
                up.text = "Upload: %.2f KB/s".format(uploadSpeed / 1024)
            }

            uploadKoneksi.disconnect()
        } catch (e: Exception) {
            runOnUiThread {
                down.text = "Error: ${e.message}"
                up.text = "Error: ${e.message}"
            }
        }
    }.start()
    }

    private fun BukaFile(file: File) {
    val pilihan = arrayOf("Text", "Music", "Video", "Lainnya")

    AlertDialog.Builder(this)
        .setTitle("Pilih Jenis File")
        .setItems(pilihan) { _, which ->
            when (which) {
                0 -> {
                    val intent = Intent(this, Editor::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this, Musik::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(this, Video::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    startActivity(intent)
                }
                3 -> { 
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, getMimeType(file.extension))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        .setNegativeButton("Batal", null)
        .show()
    }
    
    private var server: MyNanoHttpd? = null

    private fun MulaiHost() {
    startService(Intent(this, ServerService::class.java))
    val alamat = findViewById<TextView>(R.id.alamat)

    val alat = listOf(
        "login.html", "login.css", "login.js",
        "register.html", "register.css", "register.js",
        "beranda.html", "beranda.css", "beranda.js"
    )
    
    val targetDir = File(filesDir, "memogo")
    if (!targetDir.exists()) targetDir.mkdirs()
    
    alat.forEach { filename ->
        assets.open(filename).use { input ->
            File(targetDir, filename).outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    if (server == null) {
    server = MyNanoHttpd(8080, targetDir)
    server?.start()
    Log.d("SERVER", "Server mulai di: http://localhost:8080/")
    } else {
    Log.d("SERVER", "Server sudah berjalan")
    }

    alamat.text = "Server berjalan di: http://localhost:8080/login.html"
    }

    private fun StopHost() {
    stopService(Intent(this, ServerService::class.java))
        val alamat = findViewById<TextView>(R.id.alamat)
        alamat.text = "Server Mati"

        server?.stop()
        server = null
    }
    
    private fun kontrolServer(hidup: Boolean) {
    val intent = Intent(this, ServerService::class.java).apply {
        action = if (hidup) "START" else "STOP"
    }

    if (hidup) { startService(intent) }
    else { stopService(intent) }

    val alamat = findViewById<TextView>(R.id.alamat)
    alamat.text = if (hidup) "Server berjalan di: http://localhost:8080/login.html" else "Server Mati"
}
    
    //logika
}