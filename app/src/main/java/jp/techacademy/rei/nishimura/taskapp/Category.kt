package jp.techacademy.rei.nishimura.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.Date

open class Category : RealmObject(), Serializable {
    var name: String = ""     // カテゴリの名前

    // idをプライマリーキーとして設定
    @PrimaryKey
    var id: Int = 1
}