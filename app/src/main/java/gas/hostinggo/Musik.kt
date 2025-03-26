package gas.hostinggo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*

class Musik : AppCompatActivity(){
override fun onCreate(savedInstanceState: Bundle?){
    super.onCreate(savedInstanceState)
        setContentView(R.layout.musik)
        
        findViewById<TextView>(R.id.keluar).setOnClickListener{finish()}
    }
}