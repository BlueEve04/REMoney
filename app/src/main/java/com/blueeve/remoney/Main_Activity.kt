package com.blueeve.remoney

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Device(val id: Int, val name: String, val price: Int, val buy_time: String)
data class DeviceList(val devices: List<Device>)

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SquareAdapter
    private val items = mutableListOf<Device>()
    private var isEditMode = false // 用于跟踪编辑模式状态
    private var selectedDate: Calendar? = null // 用于存储选择的日期

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide() // 隐藏默认 ActionBar

        recyclerView = findViewById(R.id.recyclerView)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val fabEdit = findViewById<FloatingActionButton>(R.id.fabEdit)
        val fabAbout = findViewById<FloatingActionButton>(R.id.fabAbout)

        adapter = SquareAdapter(items) { device ->
            if (isEditMode) {
                toggleSelection(device)
            } else {
                showEditDeviceDialog(device) // 使用新的编辑对话框
            }
        }

        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = adapter

        fabAbout.setOnClickListener {
            showAboutInfo()
        }

        fabAdd.setOnClickListener {
            showNewDeviceDialog()
        }

        fabEdit.setOnClickListener {
            isEditMode = !isEditMode // 切换编辑模式
            if (isEditMode) {
                fabEdit.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // 更改按钮图标为关闭
                adapter.enableSelectionMode()
            } else {
                fabEdit.setImageResource(android.R.drawable.ic_menu_edit) // 恢复按钮图标
                adapter.deleteSelectedItems() // 批量删除选中的设备
                saveAllDevices() // 保存所有设备到文件
                adapter.disableSelectionMode() // 退出编辑模式
            }
        }

        // 读取 JSON 文件并更新 RecyclerView
        readJson()
    }

    private fun showAboutInfo() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("关于和赞赏")

        val about = layoutInflater.inflate(R.layout.dialog_about, null)
        builder.setView(about)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)

        // 显示弹窗
        dialog.show()
    }

    private fun toggleSelection(device: Device) {
        adapter.toggleSelection(device)
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun saveJson(device: Device) {
        val deviceList = parseJson(this, "data.json")?.devices?.toMutableList() ?: mutableListOf()
        deviceList.add(device)
        val jsonString = Gson().toJson(DeviceList(deviceList))
        saveJsonToFile(this, "data.json", jsonString)
        Toast.makeText(this, "JSON 已保存", Toast.LENGTH_SHORT).show()
    }

    private fun readJson() {
        val deviceList = parseJson(this, "data.json")
        deviceList?.devices?.let {
            items.clear()
            items.addAll(it)
            adapter.notifyDataSetChanged() // 更新适配器
        }
    }

    private fun readJsonFromFile(context: Context, fileName: String): String? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    private fun parseJson(context: Context, fileName: String): DeviceList? {
        val json = readJsonFromFile(context, fileName)
        return json?.let {
            Gson().fromJson(it, DeviceList::class.java)
        }
    }

    private fun saveJsonToFile(context: Context, fileName: String, jsonData: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(jsonData)
    }

    private fun showEditDeviceDialog(device: Device) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("编辑设备信息")

        val view = layoutInflater.inflate(R.layout.dialog_edit_device, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)
        val buyTimeEditText = view.findViewById<EditText>(R.id.editTextBuyTime)

        // 设置当前设备信息
        nameEditText.setText(device.name)
        priceEditText.setText(device.price.toString())
        buyTimeEditText.setText(device.buy_time)

        buyTimeEditText.setOnClickListener {
            showDatePickerDialog(buyTimeEditText)
        }

        builder.setView(view)

        // 创建对话框
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)
        }

        // 保存按钮的点击事件
        view.findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            try {
                val updatedDevice = Device(
                    id = device.id,
                    name = nameEditText.text.toString(),
                    price = priceEditText.text.toString().toInt(),
                    buy_time = buyTimeEditText.text.toString()
                )
                val position = items.indexOf(device)
                adapter.updateItem(position, updatedDevice)
                saveAllDevices() // 保存所有设备到文件
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "价格必须是有效的数字", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 删除按钮的点击事件
        view.findViewById<Button>(R.id.buttonDelete).setOnClickListener {
            val position = items.indexOf(device)
            adapter.removeItem(position)
            saveAllDevices() // 保存所有设备到文件
            dialog.dismiss()
        }

        // 取消按钮的点击事件
        view.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNewDeviceDialog() {
        val builder = AlertDialog.Builder(this)
        Toast.makeText(this, "日期格式Y-M-D，请尝试点击两次“购买时间”来选取日期", Toast.LENGTH_SHORT).show()

        val view = layoutInflater.inflate(R.layout.dialog_edit_device, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val priceEditText = view.findViewById<EditText>(R.id.editTextPrice)
        val buyTimeEditText = view.findViewById<EditText>(R.id.editTextBuyTime)

        buyTimeEditText.setOnClickListener {
            showDatePickerDialog(buyTimeEditText)
        }

        builder.setView(view)

        // 创建对话框
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)
        }

        // 设置添加按钮的点击事件
        view.findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            try {
                val newDevice = Device(
                    id = items.size + 1,
                    name = nameEditText.text.toString(),
                    price = priceEditText.text.toString().toInt(),
                    buy_time = buyTimeEditText.text.toString()
                )
                adapter.addItem(newDevice)
                saveJson(newDevice) // 将新设备保存到 JSON 文件
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "发生错误，请检查输入内容并重试", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置取消按钮的点击事件
        view.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePickerDialog(buyTimeEditText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // 更新 EditText 显示选择的日期
            selectedDate = Calendar.getInstance()
            selectedDate?.set(selectedYear, selectedMonth, selectedDay)
            buyTimeEditText.setText(String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay))
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun saveAllDevices() {
        val jsonString = Gson().toJson(DeviceList(items))
        saveJsonToFile(this, "data.json", jsonString)
        Toast.makeText(this, "设备信息已保存", Toast.LENGTH_SHORT).show()
    }
}
