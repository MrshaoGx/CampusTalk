package com.mrsgx.campustalk.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mrsgx.campustalk.data.local.TableArea.Companion.AREACODE
import com.mrsgx.campustalk.data.local.TableArea.Companion.AREANAME
import com.mrsgx.campustalk.data.local.TableArea.Companion.TABLE_AREA_NAME
import com.mrsgx.campustalk.data.local.TableLocation.Companion.LATITUDE
import com.mrsgx.campustalk.data.local.TableLocation.Companion.LONGITUDE
import com.mrsgx.campustalk.data.local.TableLocation.Companion.TABLE_LOCATION_NAME
import com.mrsgx.campustalk.data.local.TableLocation.Companion.TIME
import com.mrsgx.campustalk.data.local.TableSchool.Companion.TABLE_SCHOOL_NAME
import com.mrsgx.campustalk.data.local.TableUser.Companion.AGE
import com.mrsgx.campustalk.data.local.TableUser.Companion.EMAIL
import com.mrsgx.campustalk.data.local.TableUser.Companion.HEADPIC
import com.mrsgx.campustalk.data.local.TableUser.Companion.NICKNAME
import com.mrsgx.campustalk.data.local.TableUser.Companion.PASSWORD
import com.mrsgx.campustalk.data.local.TableUser.Companion.SCHOOLCODE
import com.mrsgx.campustalk.data.local.TableUser.Companion.SEX
import com.mrsgx.campustalk.data.local.TableUser.Companion.STATE
import com.mrsgx.campustalk.data.local.TableUser.Companion.STUCARD
import com.mrsgx.campustalk.data.local.TableUser.Companion.TABLE_USER_NAME
import com.mrsgx.campustalk.data.local.TableUser.Companion.USEREXPLAIN
import com.mrsgx.campustalk.obj.CTArea
import com.mrsgx.campustalk.obj.CTLocation
import com.mrsgx.campustalk.obj.CTSchool
import com.mrsgx.campustalk.obj.CTUser
import java.lang.ref.WeakReference

/**
 * Created by Shao on 2017/10/1.
 * nothing shows up
 */
class DB(private val context: Context) {
    private var db: SQLiteDatabase? = null

    init {
        if(db==null)
        db = SQLiteHelper(context, "campustalk").writableDatabase
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: DB? = null
        fun getInstance(context: Context): DB {
            synchronized(this){
            if (INSTANCE == null|| INSTANCE==null) {
                INSTANCE =DB(context)
            }
            }
            return INSTANCE!!
        }
    }

    fun Close() {
        if (db != null)
            db!!.close()
    }

    private fun connDB() {
        db!!.beginTransaction()
    }

    private fun disconnDB() {
        if (db != null) {
            db!!.setTransactionSuccessful()
            db!!.endTransaction()
        }
    }

    //增删改
    private fun exesql(sql: String): Boolean {
        synchronized(this) {
            try {
                connDB()
                db!!.execSQL(sql)
            } catch (e: Exception) {
                disconnDB()
                return false
            }
            disconnDB()
        }
        return true
    }

    //查询
    private fun querySql(sql: String): Cursor? {
        val rs: Cursor
        try {
            connDB()
            rs = db!!.rawQuery(sql, null)
        } catch (e: Exception) {
            return null
        }
        return rs
    }


    //===========================================业务开始=======================================//

    //增删改查用户表
    private fun insertUser(user: CTUser): Boolean {
        val sql = "insert into $TABLE_USER_NAME ($EMAIL,${TableUser.UID},$NICKNAME,$PASSWORD,$SEX,$AGE,$SCHOOLCODE,$USEREXPLAIN,$STUCARD,$STATE,$HEADPIC)" +
                " values ('${user.Email}','${user.Uid}','${user.Nickname}','${user.Password}','${user.Sex}','${user.Age}','${user.School!!.SCode}','${user.Userexplain}'," +
                "'${user.Stucard}','${user.State}','${user.Headpic}')"
        return exesql(sql)
    }

    fun updateUser(user: CTUser): Boolean {
        val sql = "update $TABLE_USER_NAME set $NICKNAME='${user.Nickname}',$AGE='${user.Age}',$USEREXPLAIN='${user.Userexplain}',$HEADPIC='${user.Headpic}',$STUCARD='${user.Stucard}',$STATE='${user.State}' " +
                "where ${TableUser.UID}='${user.Uid}'"
        return exesql(sql)
    }

    fun insertOrUpdateUser(user: CTUser): Boolean {
        val sql = "select * from $TABLE_USER_NAME where ${TableUser.UID}='${user.Uid}'"
        val cur = querySql(sql)
        var count = 0
        if (cur != null) {
            while (cur.moveToNext())
                count++
            cur.close()
        }
        disconnDB()
        return if (count >= 1) updateUser(user) else insertUser(user)
    }

    fun getUserState(uid: String): String {
        val sql = "select ${TableUser.STATE} from $TABLE_USER_NAME where ${TableUser.UID}='$uid'"
        val cur = querySql(sql)
        var result = ""
        if (cur != null) {
            while (cur.moveToNext()) {
                result = cur.getString(cur.getColumnIndex(TableUser.STATE))
            }
            cur.close()
        }
        disconnDB()
        return result
    }

    fun getLocalUser(email: String): CTUser {
        val user = CTUser()
        val sql = "select * from $TABLE_USER_NAME where $EMAIL='$email'"
        val cur = querySql(sql)
        if (cur != null) {
            if (cur.moveToLast()) {
                user.Uid = cur.getString(cur.getColumnIndex(TableUser.UID))
                user.Email = cur.getString(cur.getColumnIndex(EMAIL))
                user.Nickname = cur.getString(cur.getColumnIndex(NICKNAME))
                user.Password = cur.getString(cur.getColumnIndex(PASSWORD))
                user.Sex = cur.getString(cur.getColumnIndex(SEX))
                user.Age = cur.getString(cur.getColumnIndex(AGE))
                val school = querySchoolBySchoolCode(cur.getString(cur.getColumnIndex(SCHOOLCODE)))
                user.School = school
                user.Userexplain = cur.getString(cur.getColumnIndex(USEREXPLAIN))
                user.Stucard = cur.getString(cur.getColumnIndex(STUCARD))
                user.State = cur.getString(cur.getColumnIndex(STATE))
                user.Headpic = cur.getString(cur.getColumnIndex(HEADPIC))

            }
            cur.close()
        }
        disconnDB()
        return user
    }

    //增查地区信息
    fun insertArea(list: ArrayList<CTArea>?): Boolean {
        if (list == null)
            return false
        if (list.size < 1) {
            return false
        }
        var sql = "insert into $TABLE_AREA_NAME values "
        for (area in list) {
            sql += "('${area.Areacode}','${area.Areaname}'),"
        }
        sql = sql.substring(0, sql.length - 1)
        return exesql(sql)
    }

    fun queryArea(): ArrayList<CTArea> {
        val result = ArrayList<CTArea>()
        val sql = "select * from $TABLE_AREA_NAME"
        val cursor = querySql(sql)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val area = CTArea()
                area.Areacode = cursor.getString(cursor.getColumnIndex(AREACODE))
                area.Areaname = cursor.getString(cursor.getColumnIndex(AREANAME))
                result.add(area)
            }
            cursor.close()
        }
        disconnDB()
        return result
    }

    //增查学校信息
    fun insertSchool(list: ArrayList<CTSchool>?): Boolean {
        if (list == null)
            return false
        if (list.size <= 0)
            return false
        var sql = "insert into $TABLE_SCHOOL_NAME values"
        for (school in list) {
            sql += "('${school.SCode}','${school.SName}','${school.Areacode}'),"
        }
        sql = sql.substring(0, sql.length - 1)
        return exesql(sql)
    }

    fun querySchoolByAreaCode(areacode: String): ArrayList<CTSchool> {
        val result = ArrayList<CTSchool>()
        val sql = "select * from $TABLE_SCHOOL_NAME where ${TableSchool.AREACODE}='$areacode'"
        val cursor = querySql(sql)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val school = CTSchool()
                school.Areacode = cursor.getString(cursor.getColumnIndex(TableSchool.AREACODE))
                school.SName = cursor.getString(cursor.getColumnIndex(TableSchool.SCHOOL_NAME))
                school.SCode = cursor.getString(cursor.getColumnIndex(TableSchool.SCHOOL_CODE))
                result.add(school)
            }
            cursor.close()
        }
        disconnDB()
        return result
    }

    fun querySchoolBySchoolCode(schoolcode: String): CTSchool {
        val school = CTSchool()
        val sql = "select * from $TABLE_SCHOOL_NAME where ${TableSchool.SCHOOL_CODE}='$schoolcode'"
        val cur = querySql(sql)
        if (cur != null && cur.moveToLast()) {
            school.SName = cur.getString(cur.getColumnIndex(TableSchool.SCHOOL_NAME))
            school.SCode = cur.getString(cur.getColumnIndex(TableSchool.SCHOOL_CODE))
            school.Areacode = cur.getString(cur.getColumnIndex(TableSchool.AREACODE))
            cur.close()
        }
        disconnDB()
        return school
    }

    //增查位置信息
    fun insertLocation(loc: CTLocation) {
        if(loc.Uid.isEmpty())
        {
            return
        }
        val sql = "insert into $TABLE_LOCATION_NAME ($LATITUDE,$LONGITUDE,${TableLocation.UID},$TIME) values ('${loc.Latitude}','${loc.Longitude}','${loc.Uid}','${loc.Datetime}')"
        exesql(sql)
    }

    fun getAllLocation(): ArrayList<CTLocation> {
        val list = ArrayList<CTLocation>()
        val sql = "select * from $TABLE_LOCATION_NAME desc limit 0,10"
        try {
            val cur = querySql(sql)
            if (cur != null) {
                while (cur.moveToNext()) {
                    val loc=CTLocation()
                    loc.Uid=cur.getString(cur.getColumnIndex(TableLocation.UID))
                    loc.Datetime=cur.getString(cur.getColumnIndex(TableLocation.TIME))
                    loc.Latitude=cur.getString(cur.getColumnIndex(TableLocation.LATITUDE))
                    loc.Longitude=cur.getString(cur.getColumnIndex(TableLocation.LONGITUDE))
                    list.add(loc)
                }
                cur.close()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        disconnDB()
        return list
    }

    fun clearLocation(){
        val sql="delete from $TABLE_LOCATION_NAME where ${TableLocation.UID} in (select ${TableLocation.UID} from $TABLE_LOCATION_NAME desc limit 0,10)"
        exesql(sql)
    }
}