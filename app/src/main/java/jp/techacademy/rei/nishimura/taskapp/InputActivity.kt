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
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.content_input.*
import kotlinx.android.synthetic.main.content_input.add_button
import java.util.*

class InputActivity : AppCompatActivity() {

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null
    private var mCategory: Category? = Category()

    private var isTask = false

    private val mOnDateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(this,
        DatePickerDialog.OnDateSetListener { _,year,month,dayOfMonth ->
            mYear = year
            mMonth = month
            mDay = dayOfMonth
            val dateString = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            date_button.text = dateString
        },mYear, mMonth, mDay)
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                mHour = hour
                mMinute = minute
                val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)
                times_button.text = timeString
            }, mHour, mMinute, false)
        timePickerDialog.show()
    }

    // 決定ボタン
    private val mOnDoneClickListener = View.OnClickListener {
       if (isTask) {
           val title = title_edit_text.text.toString()
           val content = content_edit_text.text.toString()
           val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
           val date = calendar.time
           updateTask(title, content, date , mCategory!!.id, mTask!!.id) // 更新
       } else {
           addTask() // 追加
       }
        finish()
    }

    // 選択肢
    private var spinnerItems = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        initCategory()

        // ArrayAdapter
        val adapter = ArrayAdapter(applicationContext,
            android.R.layout.simple_spinner_item, spinnerItems)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // spinner に adapter をセット
        // Kotlin Android Extensions
        spinner.adapter = adapter


        // リスナーを登録
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                mCategory = Category()
                val spinnerParent = parent as Spinner
                mCategory!!.name = spinnerParent.selectedItem as String
                mCategory!!.id = position + 1
            }

            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }

        add_button.setOnClickListener {
            val intent = Intent(this, InputCategoryActivity::class.java)
            startActivity(intent)
        }

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // UI部品の設定
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)

        // EXTRA_TASKからTaskのidを取得して、idからTaskのインスタンスを取得する
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)

        mTask = readTask(taskId)

        if (mTask == null) {
            isTask = false
            done_button.text = "決定"
            // 新規作成の場合
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        } else {
            isTask = true
            done_button.text = "更新"
            // 更新の場合
            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)
            spinner.setSelection(mTask?.category!!.id - 1)


            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

            date_button.text = dateString
            times_button.text = timeString
        }
    }

    private fun initCategory() {
        val category = readCategory()
        for(data in category){
            spinnerItems.add(data!!.name)
        }
    }

    private fun addTask() {
        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()
        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calendar.time
        createTask(title, content, date, mCategory!!.id)
        mTask = getTask()
        val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK, mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, resultPendingIntent)
    }

    override fun onResume() {
        super.onResume()
        spinnerItems.clear()

        val categoryData = readCategory()
        for(data in categoryData){
            spinnerItems.add(data!!.name)
        }
    }

    fun createTask(taskName: String, taskContent: String, taskDate: Date, categoryId: Int){
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val resultTask = realm.where(Task::class.java).findAll()
        val taskId = resultTask.size + 1
        val resultCategory = realm.where(Category::class.java).equalTo("id", categoryId).findFirst()
        val obj = realm.createObject(Task::class.java, taskId)
        obj.title = taskName
        obj.contents = taskContent
        obj.date = taskDate
        obj.category = resultCategory
        realm.commitTransaction()
        realm.close()
    }

    fun  readTask(taskId: Int) : Task?{
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val result = realm.where<Task>().equalTo("id", taskId).findFirst()
        realm.commitTransaction()
        realm.close()
        return result
    }

    fun  getTask() : Task?{
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val results = realm.where<Task>().findAll()
        val taskId = results.size
        val result = realm.where<Task>().equalTo("id", taskId).findFirst()
        realm.commitTransaction()
        realm.close()
        return result
    }

    fun  readCategory() : List<Category?>{
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val result = realm.where<Category>().findAll()
        realm.commitTransaction()
        realm.close()
        return result
    }

    private fun updateTask(taskName: String, taskContent: String, taskDate: Date, categoryId: Int, taskId: Int) {

        Realm.getDefaultInstance()
            .executeTransactionAsync { realm ->
                val resultTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
                val resultCategory = realm.where(Category::class.java).equalTo("id", categoryId).findFirst()
                resultTask?.title = taskName
                resultTask?.contents = taskContent
                resultTask?.date = taskDate
                resultTask?.category = resultCategory
            }
    }

}