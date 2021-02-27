package jp.techacademy.rei.nishimura.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.spinner
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

const val EXTRA_TASK = "jp.jp.techacademy.rei.nishimura.taskapp"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter
    private var spinnerItems = arrayListOf<String>()

    private var selectCategoryId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCategory()

        // ArrayAdapter
        val adapter = ArrayAdapter(applicationContext,
            android.R.layout.simple_spinner_item, spinnerItems)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // spinner に adapter をセット
        spinner.adapter = adapter

        // リスナーを登録
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                selectCategoryId = position
                // 上の結果を、TaskListとしてセットする
                mTaskAdapter.mTaskList = searchTask(selectCategoryId)

                // TaskのListView用のアダプタに渡す
                listView1.adapter = mTaskAdapter

                // 表示を更新するために、アダプターにデータが変更されたことを知らせる
                mTaskAdapter.notifyDataSetChanged()

            }

            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }

        fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK") { _, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults =
            mRealm.where(Task::class.java).findAll().sort("date", Sort.ASCENDING)

        // 上の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    fun categoryQuery(query: String) {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に指定されたカテゴリを並べた結果」を取得
        val taskRealmResults =
            mRealm.where(Task::class.java).contains("category",  query)
                .findAll().sort("date", Sort.ASCENDING)

        // 上の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun initCategory() {
        val realm = Realm.getDefaultInstance()

        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val category =
            realm.where(Category::class.java).findAll()

        if (category.size == 0 ){
            val categoryArray = arrayOf("カテゴリ未登録","家事", "勉強", "趣味")
            for (category in categoryArray){
                createCategory(category)
            }
        }

        val value = realm.where(Category::class.java).findAll()
        val realmData = realm.copyFromRealm(value)
        for(data in realmData){
            spinnerItems.add(data.name)
        }

    }

    fun createCategory(categoryName: String){
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val resultCategory = realm.where(Category::class.java).findAll()
        val categoryId = resultCategory.size + 1
        val obj = realm.createObject(Category::class.java, categoryId)
        obj.name = categoryName
        realm.commitTransaction()
        realm.close()
    }

       override fun onResume() {
        super.onResume()
        spinnerItems.clear()

           // 上の結果を、TaskListとしてセットする
           mTaskAdapter.mTaskList = searchTask(selectCategoryId)

           // TaskのListView用のアダプタに渡す
           listView1.adapter = mTaskAdapter

           // 表示を更新するために、アダプターにデータが変更されたことを知らせる
           mTaskAdapter.notifyDataSetChanged()

        val categoryData = readCategory()
        for(data in categoryData){
            spinnerItems.add(data!!.name)
        }
           spinnerItems.add(0,"タスク一覧")
    }

    fun  readCategory() : List<Category?>{
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val result = realm.where<Category>().findAll()
        realm.commitTransaction()
        realm.close()
        return result
    }

    fun searchTask(categoryId: Int) : MutableList<Task> {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val result = if (categoryId == 0){
             realm.where<Task>().findAll()
        } else {
            realm.where<Task>().equalTo("category.id", categoryId).findAll()
        }
        realm.commitTransaction()
        realm.close()
        return result
    }
}
