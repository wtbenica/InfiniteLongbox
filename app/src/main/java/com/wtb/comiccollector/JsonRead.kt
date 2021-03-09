package com.wtb.comiccollector

import android.util.JsonReader
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

class JsonRead() {
    fun readJsonStream(str_in: InputStream): ArrayList<Item> {
        val reader = JsonReader(InputStreamReader(str_in, "UTF-8"))
        try {
            return readItemsArray(reader)
        } finally {
            reader.close()
        }
    }

    private fun readItemsArray(reader: JsonReader): ArrayList<Item> {
        val items = arrayListOf<Item>()

        reader.beginArray()
        while (reader.hasNext()) {
            items.add(readItem(reader))
        }
        reader.endArray()
        return items
    }

    private fun readItem(reader: JsonReader): Item {
        var model: String? = null
        var pk: Int = -1
        var data: Item.Fields? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when (name) {
                "model" -> model = reader.nextString()
                "pk" -> pk = reader.nextInt()
                "data" -> data = readData(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Item(model, pk, data)
    }

    private fun readData(reader: JsonReader): Item.Fields {
        var name: String? = null
        var pub_dates: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val field_name = reader.nextName()
            when (field_name) {
                "name" -> name = reader.nextString()
                "pub_dates" -> pub_dates = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Item.Fields(name, pub_dates)
    }

    fun main() {
        val inputStream: InputStream = FileInputStream("series.json")
        val itemList = readJsonStream(inputStream)
        for (item in itemList) {
            print(item)
        }
    }

    class Item(
        @SerializedName("model")
        @Expose
        var model: String?,
        @SerializedName("pk")
        @Expose
        var pk: Int,
        @SerializedName("fields")
        @Expose
        var fields: Fields?,
    ) {
        class Fields(
            @SerializedName("name")
            @Expose
            var name: String?,
            @SerializedName("publication_dates")
            var publicationDates: String?,
        )
    }
}
