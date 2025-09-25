package com.recycling.toolsapp.ui

import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.recycling.toolsapp.R
import com.recycling.toolsapp.adapter.ItemReportingBoxAdapter
import com.recycling.toolsapp.adapter.ItemReportingBoxClickListener
import com.recycling.toolsapp.adapter.ItemSpacingDecoration
import com.recycling.toolsapp.databinding.FragmentReportingBoxBinding
import com.recycling.toolsapp.fitsystembar.base.bind.BaseBindFragment
import com.recycling.toolsapp.http.FileCleaner
import com.recycling.toolsapp.http.MailConfig
import com.recycling.toolsapp.http.MailSender
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.Loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/***
 * 下发数据查询
 */
@AndroidEntryPoint class ReportingBoxFragment : BaseBindFragment<FragmentReportingBoxBinding>() {
    // 关键点：通过 requireActivity() 获取 Activity 作用域的 ViewModel// 确保共享实例
    private val cabinetVM: CabinetVM by viewModels(ownerProducer = { requireActivity() })
    var mItemReportingBoxAdapter: ItemReportingBoxAdapter? = null

    private var reportingBoxs = mutableListOf<String>()

    override fun layoutRes(): Int {
        return R.layout.fragment_reporting_box
    }

    override fun isShowActionBar(): Boolean {
        return true
    }

    override fun isShowActionBarBack(): Boolean {
        return true
    }


    override fun initialize(savedInstanceState: Bundle?) {
        getReportingBox()
        initRecycle()
        initSearch()
    }

    private fun initSearch() {
        binding.logSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    mItemReportingBoxAdapter?.updateItems(reportingBoxs)
                    return
                }
                val query = binding.logSearch.text.toString()
                val result = reportingBoxs.filter { item ->
                    item.contains(query, ignoreCase = true)
                }

                mItemReportingBoxAdapter?.updateItems(result)

            }
        })
        binding.aivUpdate.setOnClickListener {
            getReportingBox()
            zipFile()
        }

    }

    //压缩文件发送至邮箱
    private fun zipFile() {
        cabinetVM.ioScope.launch {
            // 目标文件夹路径
            val targetFolder =
                    File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "socket_box_crash")
            // 压缩包输出路径
            val zipOutput =
                    File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${AppUtils.getDateYMDHMS()}socket_box_crash.zip")

            // 执行压缩
            val success = FileCleaner.zipFolder(targetFolder.absolutePath, zipOutput.absolutePath)

            if (success) {
                // 压缩成功处理
                val mailConfig = MailConfig.Builder().apply {
                    host = "smtp.qq.com"
                    port = 587
//                    port = 465
                    username = "860023654@qq.com"
                    password = "raiszbpinaznbbjd" // 或 oauthToken("ya29.token")
                    setRecipient("860023654@qq.com")
                    setSubject("查看下位机上报数据")
                    setBody("<b>查看附件下位机上报数据</b>")
                    setAttach(zipOutput)
                }.build()
                when (val result = MailSender.sendDirectly(mailConfig)) {
                    is MailSender.Result.Success -> {
                        cabinetVM.tipMessage("发送邮件成功")
                    }

                    is MailSender.Result.Failure -> {
                        cabinetVM.tipMessage("发送邮件 ${result.exception}")
                    }
                }
            } else {
                // 压缩失败处理
            }
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun getReportingBox() {
        cabinetVM.ioScope.launch {
            reportingBoxs.clear()
            val result = BoxToolLogUtils.listBoxInfoFiles()
            if (result?.isNotEmpty() == true) {
                // 按日期降序排序（最新日期在前）
                val sortedFiles = result.sortedByDescending {
                    val datePart = it.substring(it.lastIndexOf("--") + 2, it.lastIndexOf(".txt"))
                    dateFormat.parse(datePart)
                }
                reportingBoxs.addAll(sortedFiles)
            }
            withContext(Dispatchers.Main) {
                mItemReportingBoxAdapter?.updateItems(reportingBoxs)

            }
        }
    }

    private fun initRecycle() {
        mItemReportingBoxAdapter = ItemReportingBoxAdapter(reportingBoxs)
        binding.recycler.apply {
            adapter = mItemReportingBoxAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(ItemSpacingDecoration(AppUtils.getContext().resources.getDimensionPixelSize(R.dimen.dp_5)))
        }
        mItemReportingBoxAdapter?.addItemReportingBoxClickListener(object : ItemReportingBoxClickListener {
            override fun itemClick(fileName: String) {
                val file =
                        File(AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "socket_box_crash/${fileName}")
                Loge.d("点击进来了 ${file.absolutePath}")
                cabinetVM.ioScope.launch {
                    val mailConfig = MailConfig.Builder().apply {
                        host = "smtp.qq.com"
                        port = 587
//                    port = 465
                        username = "860023654@qq.com"
                        password = "raiszbpinaznbbjd" // 或 oauthToken("ya29.token")
                        setRecipient("860023654@qq.com")
                        setSubject("查看所有通信日志")
                        setBody("<b>查看所有通信日志</b>")
                        setAttach(file)
                    }.build()
                    when (val result = MailSender.sendDirectly(mailConfig)) {
                        is MailSender.Result.Success -> {
                            cabinetVM.tipMessage("发送邮件成功")
                        }

                        is MailSender.Result.Failure -> {
                            cabinetVM.tipMessage("发送邮件 ${result.exception}")
                        }
                    }
                }
            }
        })
    }
}
