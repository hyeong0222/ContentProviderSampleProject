package com.example.contentprovidersampleproject

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri

class SampleContentProvider : ContentProvider() {

    private var uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    init {
        /**
         * UriMatcher는 Uri가 일치하는지 판단해주는 역할. addUri를 통해서 비교하고 싶은 uri를 등록.
         *
         * 만약 uri가 Uri.parse("content://com.example.contentprovidersample.provider/cheeses") 이라면,
         * uriMatcher.match(uri)는 CODE_CHEESE_DIR 리턴.
         *
         * 만약 uri가 Uri.parse("content://com.example.contentprovidersample.provider/cheeses/5") 이라면,
         * uriMatcher.match(uri)는 CODE_CHEESE_ITEM 리턴.
         *
         * 만약 매치값이 없다면 -1 리턴.
         */
        uriMatcher.addURI(AUTHORITY, Cheese.TABLE_NAME, CODE_CHEESE_DIR)
        uriMatcher.addURI(AUTHORITY, "${Cheese.TABLE_NAME}/#", CODE_CHEESE_ITEM)
    }


    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            CODE_CHEESE_DIR, CODE_CHEESE_ITEM -> {
                val queryBuilder = SQLiteQueryBuilder()
                queryBuilder.tables = Cheese.TABLE_NAME
                val db = SampleDatabase.getInstance(context!!)
                val cursor = queryBuilder.query(
                    db, projection, selection, selectionArgs, null, null, sortOrder)
                cursor.setNotificationUri(context!!.contentResolver, uri)
                cursor
            }
            else -> {
                throw IllegalArgumentException("Unknown URI: $uri")
            }
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_CHEESE_DIR -> "vnd.android.cursor.dir/$AUTHORITY.$Cheese.TABLE_NAME"
            CODE_CHEESE_ITEM -> "vnd.android.cursor.item/$AUTHORITY.$Cheese.TABLE_NAME"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * Uri가 테이블 전체를 의미하는 .../cheeses 형태로 들어온다면 insert 수행.
     * Uri가 특정 ID를 의미하는 .../cheeses/5 형태로 들어온다면 exception 발생.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uriMatcher.match(uri)) {
            CODE_CHEESE_DIR -> {
                val id = SampleDatabase.getInstance(context!!)
                    .insert(Cheese.TABLE_NAME, null, values)
                val insertedUri = ContentUris.withAppendedId(uri, id)
                context!!.contentResolver.notifyChange(insertedUri, null)
                insertedUri
            }
            CODE_CHEESE_ITEM -> {
                throw java.lang.IllegalArgumentException("Invalid URI, cannot insert with ID: $uri")
            }
            else -> {
                throw java.lang.IllegalArgumentException("Unknown URI: $uri")
            }
        }    }

    /**
     * Uri가 테이블 전체를 의미하는 .../cheeses 형태로 들어온다면 exception 발생.
     * Uri가 특정 ID를 의미하는 .../cheeses/5 형태로 들어온다면 delete 수행.
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            CODE_CHEESE_DIR -> {
                throw java.lang.IllegalArgumentException("Invalid URI, cannot update without ID: $uri")
            }
            CODE_CHEESE_ITEM -> {
                val id = ContentUris.parseId(uri)
                val count = SampleDatabase.getInstance(context!!)
                    .delete(Cheese.TABLE_NAME,
                        "${Cheese.COLUMN_ID} = ?",
                        arrayOf(id.toString()))
                context!!.contentResolver.notifyChange(uri, null)
                count
            }
            else -> {
                throw java.lang.IllegalArgumentException("Unknown URI: $uri")
            }
        }
    }

    /**
     * Uri가 테이블 전체를 의미하는 .../cheeses 형태로 들어온다면 exception 발생.
     * Uri가 특정 ID를 의미하는 .../cheeses/5 형태로 들어온다면 update 수행.
     */
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            CODE_CHEESE_DIR -> {
                throw java.lang.IllegalArgumentException("Invalid URI, cannot update without ID$uri")
            }
            CODE_CHEESE_ITEM -> {
                val id = ContentUris.parseId(uri)
                val count = SampleDatabase.getInstance(context!!)
                    .update(Cheese.TABLE_NAME, values, "${Cheese.COLUMN_ID} = ?",
                        arrayOf(id.toString()))
                context!!.contentResolver.notifyChange(uri, null)
                count
            }
            else -> {
                throw java.lang.IllegalArgumentException("Unknown URI: $uri")
            }
        }
    }

    companion object {
        const val AUTHORITY = "com.example.contentprovidersampleproject.provider"
        val URI_CHEESE: Uri = Uri.parse(
            "content://" + AUTHORITY + "/" + Cheese.TABLE_NAME)
        const val CODE_CHEESE_DIR = 1
        const val CODE_CHEESE_ITEM = 2
    }

}