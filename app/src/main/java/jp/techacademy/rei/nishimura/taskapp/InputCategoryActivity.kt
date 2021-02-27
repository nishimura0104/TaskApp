package jp.techacademy.rei.nishimura.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.category_input.*
import kotlinx.android.synthetic.main.content_input.*
import kotlinx.android.synthetic.main.content_input.add_button
import java.util.*

class InputCategoryActivity : AppCompatActivity() {

        private var mCategory: Category? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.category_input)

            // ActionBarを設定する
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)
            if (supportActionBar != null) {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }

            add_button.setOnClickListener {
                addCategory()
                finish()
            }
        }

    private fun addCategory() {
        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

            // 新規作成の場合
            mCategory = Category()

            val category = realm.where(Category::class.java).findAll()

            val  identifier: Int =
                if (category.max("id") != null) {
                    category.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mCategory!!.id = identifier


        val name = category_edit_text.text.toString()

        mCategory!!.name = name

        realm.copyToRealmOrUpdate(mCategory!!)
        realm.commitTransaction()

        realm.close()

    }

    fun createCategory(categoryName: String){
        Realm.getDefaultInstance()
            .executeTransactionAsync { realm ->
                val resultCategory = realm.where(Category::class.java).findAll()
                val categoryId = resultCategory.size + 1
                val obj = realm.createObject(Category::class.java, categoryId)
                obj.name = categoryName
            }
    }
}
