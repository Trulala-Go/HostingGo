package gas.hostinggo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import android.view.View
import java.io.File
import android.content.Intent
import android.text.TextWatcher
import android.text.Editable
import java.util.Stack

class Editor : AppCompatActivity() {
    private lateinit var nama: TextView
    private lateinit var tulis: EditText
    private var file: File? = null
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoRedo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editor)

        findViewById<TextView>(R.id.keluar).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.nav).setOnClickListener {
            val liner = findViewById<LinearLayout>(R.id.liner)
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        nama = findViewById(R.id.nama)
        tulis = findViewById(R.id.tulis)

        val path = intent.getStringExtra("file_path")
        if (path != null) {
            file = File(path)
            nama.text = file!!.name
            tulis.setText(file!!.readText())
            undoStack.push(tulis.text.toString())
        }

        findViewById<ImageView>(R.id.simpan).setOnClickListener {
            if (file != null) {
                try {
                    file!!.writeText(tulis.text.toString())
                    Toast.makeText(this, "File disimpan!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menyimpan file!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tulis.addTextChangedListener(object : TextWatcher {
            private var previousText = tulis.text.toString()
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (isUndoRedo) {
                    isUndoRedo = false
                    return
                }
                
                val currentText = tulis.text.toString()
                if (currentText == previousText) return
                
                val diff = findChangedWord(previousText, currentText)
                if (diff != null) {
                    undoStack.push(previousText)
                    redoStack.clear()
                }
                
                previousText = currentText
            }
            
            private fun findChangedWord(oldText: String, newText: String): Pair<String, String>? {
                val oldWords = oldText.split("\\s+".toRegex())
                val newWords = newText.split("\\s+".toRegex())
                
                if (oldWords.size != newWords.size) {
                    return oldText to newText
                }
                
                for (i in oldWords.indices) {
                    if (oldWords[i] != newWords[i]) {
                        return oldWords[i] to newWords[i]
                    }
                }
                
                return null
            }
        })

        val undo = findViewById<ImageView>(R.id.undo)
        val redo = findViewById<ImageView>(R.id.redo)
        
        undo.setOnClickListener {
            if (undoStack.size > 1) {
                redoStack.push(undoStack.pop())
                isUndoRedo = true
                tulis.setText(undoStack.peek())
                tulis.setSelection(tulis.text.length)
                
                redo.visibility = View.VISIBLE
                if (undoStack.size == 1) {
                    undo.visibility = View.GONE
                }
            }
        }

        redo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                undoStack.push(redoStack.pop())
                isUndoRedo = true
                tulis.setText(undoStack.peek())
                tulis.setSelection(tulis.text.length)
                
                undo.visibility = View.VISIBLE
                if (redoStack.isEmpty()) {
                    redo.visibility = View.GONE
                }
            }
        }
        
        undo.visibility = if (undoStack.size > 1) View.VISIBLE else View.GONE
        redo.visibility = View.GONE
    }
}
