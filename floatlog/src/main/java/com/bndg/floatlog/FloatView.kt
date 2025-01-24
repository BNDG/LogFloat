/*
 * Copyright (C) 2015 pengjianbo(pengjianbosoft@gmail.com), Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.bndg.floatlog

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bndg.floatlog.OtherUtils.formatJson
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author r
 * @date 2025/1/21
 * @description 悬浮窗
 */

class FloatView(context: Context) : FrameLayout(context), OnTouchListener {
    private val HANDLER_TYPE_HIDE_LOGO = 100 //隐藏view
    private var mWmParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var mCanHide = false //是否允许隐藏
    private var mTouchStartX = 0f
    private var mTouchStartY = 0f
    private var mScreenWidth = 0f
    private var mScreenHeight = 0f
    private var mDraging = false

    private var rootFloatView: View? = null
    private var floatViewWidth = 0
    private var floatViewHeight = 0
    private var TRANSLATE_TOLEFT = false

    private var mContainView: LinearLayout? = null
    private var logViewMain: View? = null
    private var translateX: ObjectAnimator? = null
    private var mCanClose = false
    private var rv_content: RecyclerView? = null
    private var mAdapter: HttpLogAdapter? = null
    private var tv_clear: View? = null
    private var ll_search: View? = null
    private var v_outside: View? = null
    private var iv_up: View? = null
    private var iv_down: View? = null
    private var fl_contain: FrameLayout? = null
    private var nsv_scroll: NestedScrollView? = null
    private var tv_results: TextView? = null
    private var tv_toast: TextView? = null
    private var et_input: EditText? = null
    private var bt_back: View? = null
    private var mHandler: Handler? = null

    // 搜索相关
    private val matchIndexesMap = HashMap<String, List<Int>?>()
    private var currentIndex = -1 // 当前选中的匹配项索引
    private var fadeOut: AlphaAnimation? = null
    private var logEvents: ArrayList<HttpLogEvent>? = null

    init {
        init(context)
        // 使用 WeakReference 来避免内存泄漏
        val weakContext = WeakReference(context)
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == HANDLER_TYPE_HIDE_LOGO) {
                    val context = weakContext.get()
                    // 这里可以判断context是否为空，如果为空，表示Activity/Fragment已经销毁
                    if (context != null) {
                        // 执行隐藏悬浮窗的逻辑
                        if (mCanHide) {
                            mCanHide = false
                            if (TRANSLATE_TOLEFT) {
                                translateX((-floatViewWidth / 1.2).toFloat(), 500)
                            } else {
                                translateX((floatViewWidth / 1.2).toFloat(), 500)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun init(mContext: Context) {
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenSize()
        this.mWmParams = WindowManager.LayoutParams()
        // 设置窗体显示类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWmParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            mWmParams!!.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        // 设置图片格式，效果为背景透明
        mWmParams!!.format = PixelFormat.RGBA_8888
        // FLAG_NOT_TOUCH_MODAL 浮动窗口不会拦截所有触摸事件，它只响应自身的触摸事件。
        // FLAG_NOT_FOCUSABLE 窗口不会获取焦点，也不会接收键盘输入。会拦截并阻止返回键的传递
        // FLAG_LAYOUT_IN_SCREEN 使窗口布局在整个屏幕范围内，而不是限制在父容器内。这通常用于确保窗口可以覆盖整个屏幕，包括状态栏和其他系统UI元素。
        // FLAG_FULLSCREEN 窗口会覆盖状态栏和导航栏，并填充整个屏幕。这通常用于隐藏状态栏和导航栏，只显示应用程序内容。
        // TYPE_SYSTEM_ERROR 这个窗口类型用于显示系统级别的错误信息。它会覆盖所有其他应用，并且通常具有较高的优先级。 需要 SYSTEM_ALERT_WINDOW 权限。
        // FLAG_ALT_FOCUSABLE_IM 允许窗口在输入法可见的情况下仍然保持可交互状态。
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
//        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        mWmParams!!.flags = flags
        // 调整悬浮窗显示的停靠位置为左侧置顶
        mWmParams!!.gravity = Gravity.LEFT or Gravity.TOP
        // 设置悬浮窗口长宽数据
        mWmParams!!.width = LayoutParams.WRAP_CONTENT
        mWmParams!!.height = LayoutParams.WRAP_CONTENT
        addView(createFloatView(mContext))
        mWindowManager!!.addView(this, mWmParams)
        createContainView(mContext)
        mWindowManager!!.addView(mContainView, createParams())
        mContainView!!.setOnTouchListener { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                closeContentView()
            }
            true
        }
    }

    private fun updateScreenSize() {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = mWindowManager?.currentWindowMetrics
            val bounds = windowMetrics?.bounds
            displayMetrics.widthPixels = bounds?.width() ?: 0
            displayMetrics.heightPixels = bounds?.height() ?: 0
        } else {
            @Suppress("DEPRECATION")
            mWindowManager?.defaultDisplay?.getMetrics(displayMetrics)
        }
        mScreenWidth = displayMetrics.widthPixels.toFloat()
        mScreenHeight = displayMetrics.heightPixels.toFloat()
    }

    /**
     * 检查位置
     *
     * @param currentX
     * @param currentY
     */
    private fun updatePosition(currentX: Int) {
        if (currentX >= mScreenWidth / 2) {
            TRANSLATE_TOLEFT = false
            mWmParams!!.x = (mScreenWidth - floatViewWidth).toInt()
        } else if (currentX < mScreenWidth / 2) {
            TRANSLATE_TOLEFT = true
            mWmParams!!.x = 0
        }
        mWindowManager!!.updateViewLayout(this, mWmParams)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 记录当前屏幕方向转换前的宽高和悬浮窗的位置
        val oldX = mWmParams!!.x
        val oldY = mWmParams!!.y
        val displayPositionX = oldX / mScreenWidth
        val displayPositionY = oldY / mScreenHeight
        // 更新屏幕大小
        updateScreenSize()
        // 根据屏幕方向来计算新的悬浮窗位置
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                showLog("横屏 $oldX <> $oldY")
                // 计算新位置
                mWmParams!!.x = (displayPositionX * mScreenWidth).toInt()
                mWmParams!!.y = (displayPositionY * mScreenHeight).toInt()

                // 确保位置不会超出屏幕范围
                mWmParams!!.x = mWmParams!!.x.coerceIn(0, (mScreenWidth - floatViewWidth).toInt())
                mWmParams!!.y = mWmParams!!.y.coerceIn(0, (mScreenHeight - floatViewHeight).toInt())
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                showLog("竖屏 $oldX <> $oldY")

                // 计算新位置
                mWmParams!!.x = (displayPositionX * mScreenWidth).toInt()
                mWmParams!!.y = (displayPositionY * mScreenHeight).toInt()

                // 确保位置不会超出屏幕范围
                mWmParams!!.x = mWmParams!!.x.coerceIn(0, (mScreenWidth - floatViewWidth).toInt())
                mWmParams!!.y = mWmParams!!.y.coerceIn(0, (mScreenHeight - floatViewHeight).toInt())
            }
        }

        // 更新悬浮窗的布局
        mWindowManager!!.updateViewLayout(this, mWmParams)
    }

    private fun showLog(s: String) {
    }

    /**
     * 创建Float view
     *
     * @param context
     * @return
     */
    private fun createFloatView(context: Context): View? {
        // 从布局文件获取浮动窗口视图
        rootFloatView = LayoutInflater.from(context).inflate(R.layout.float_view_widget, null)
        rootFloatView?.setOnTouchListener(this)
        rootFloatView?.setOnClickListener(OnClickListener {
            if (mCanHide && !mDraging) {
                if (mContainView!!.visibility == INVISIBLE) {
                    mContainView!!.visibility = VISIBLE
                    loadAnimIn(logViewMain)
                }
            }
        })
        rootFloatView?.measure(
            MeasureSpec.makeMeasureSpec(
                0,
                MeasureSpec.UNSPECIFIED
            ), MeasureSpec
                .makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        floatViewWidth = rootFloatView?.measuredWidth ?: 0
        floatViewHeight = rootFloatView?.measuredHeight ?: 0
        return rootFloatView
    }

    /**
     * 创建contain view
     *
     */
    private fun createContainView(context: Context) {
        val inflater = LayoutInflater.from(context)
        mContainView = inflater.inflate(R.layout.float_view_contain, null) as LinearLayout
        fl_contain = mContainView!!.findViewById(R.id.fl_contain)
        v_outside = mContainView!!.findViewById(R.id.fl_outside)
        v_outside?.setOnClickListener({ closeContentView() })
        logViewMain = inflater.inflate(R.layout.float_view_home, null)
        mContainView!!.visibility = INVISIBLE
        rv_content = logViewMain?.findViewById(R.id.rv_content)
        nsv_scroll = logViewMain?.findViewById(R.id.nsv_scroll)
        tv_results = logViewMain?.findViewById(R.id.tv_results)
        tv_toast = logViewMain?.findViewById(R.id.tv_toast)
        et_input = logViewMain?.findViewById(R.id.et_log_search_input)
        bt_back = logViewMain?.findViewById(R.id.bt_back)
        tv_clear = logViewMain?.findViewById(R.id.tv_clear)
        ll_search = logViewMain?.findViewById(R.id.ll_search)
        iv_up = logViewMain?.findViewById(R.id.iv_up)
        iv_down = logViewMain?.findViewById(R.id.iv_down)
        et_input?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideSoftInput(et_input)
                val keyword = et_input?.getText().toString()
                val json = tv_results?.getText().toString()
                if (!TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(json)) {
                    onSearch(keyword)
                    findNextMatchAndScroll(keyword)
                }
                // 返回true表示我们已经处理了这个事件
                return@setOnEditorActionListener true
            }
            false // 如果不是搜索动作，则返回false
        }
        tv_results?.setOnLongClickListener(OnLongClickListener { v: View? ->
            OtherUtils.copyTextToBoard(context, tv_results?.getText().toString())
            showMsg("复制成功!")
            true
        })
        bt_back?.setOnClickListener(OnClickListener {
            slideOutFromRight(nsv_scroll)
            slideInFromLeft(rv_content)
            hideSoftInput(et_input)
        })
        iv_up?.setOnClickListener(OnClickListener { v: View? ->
            hideSoftInput(et_input)
            val keyword = et_input?.getText().toString()
            val json = tv_results?.getText().toString()
            if (!TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(json)) {
                onSearch(keyword)
                findPreviousMatchAndScroll(keyword)
            }
        })
        iv_down?.setOnClickListener(OnClickListener { v: View? ->
            hideSoftInput(et_input)
            val keyword = et_input?.getText().toString()
            val json = tv_results?.getText().toString()
            if (!TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(json)) {
                onSearch(keyword)
                findNextMatchAndScroll(keyword)
            }
        })
        logEvents = ArrayList()
        tv_clear?.setOnClickListener { v: View? ->
            logEvents!!.clear()
            mAdapter!!.notifyDataSetChanged()
        }
        rv_content?.setLayoutManager(LinearLayoutManager(context))
        mAdapter = HttpLogAdapter(logEvents!!, { event: HttpLogEvent ->
            if (!TextUtils.isEmpty(event.results)) {
                val format = formatJson(event.results)
                nsv_scroll?.scrollTo(0, 0)
                // 显示请求内容
                slideOutFromLeft(rv_content!!)
                slideInFromRight(nsv_scroll!!)
                tv_results!!.postDelayed({
                    tv_clear!!.visibility = INVISIBLE
                    ll_search!!.visibility = VISIBLE
                    et_input!!.setText("")
                    tv_results!!.text = format
                    matchIndexesMap.clear()
                }, 300)
            }
        }, { view: View, event: HttpLogEvent ->
            when (view.id) {
                R.id.tv_url -> {
                    OtherUtils.copyTextToBoard(context, event.url)
                    showMsg("复制成功!")
                }

                R.id.tv_params -> {
                    OtherUtils.copyTextToBoard(context, event.params)
                    showMsg("复制成功!")
                }
            }
        })
        rv_content?.setAdapter(mAdapter)
        fl_contain?.addView(logViewMain)
    }

    private fun hideSoftInput(view: View?) {
        val imm =
            view!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showMsg(s: String) {
        tv_toast!!.text = s
        tv_toast!!.alpha = 1f
        tv_toast!!.visibility = VISIBLE
        // 延迟1秒后开始动画
        tv_toast?.postDelayed({ // 创建渐变动画，透明度从 1 到 0
            fadeOut = AlphaAnimation(1.0f, 0.0f)
            fadeOut!!.duration = 1000 // 动画持续1秒
            fadeOut!!.fillAfter = false // 动画结束后保持最终状态
            // 设置动画监听器，在动画结束时隐藏 TextView
            fadeOut!!.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                    tv_toast!!.visibility = GONE // 动画结束后隐藏 TextView
                }

                override fun onAnimationRepeat(animation: Animation) {
                }
            })
            tv_toast!!.startAnimation(fadeOut)
        }, 1000) // 延迟1秒后执行动画
    }

    // 初始化或重新搜索时调用此方法以构建匹配项列表
    private fun buildMatchIndexes(json: String, keyword: String) {
        if (matchIndexesMap[keyword] != null) {
            return
        }
        val matchIndexes: MutableList<Int> = ArrayList()
        currentIndex = -1
        if (keyword.isEmpty()) return
        var index = 0
        while (index >= 0) {
            index = json.indexOf(keyword, index)
            if (index >= 0) {
                matchIndexes.add(index)
                index += keyword.length // 移动到下一个可能的匹配位置
            }
        }
        if (matchIndexes.isEmpty()) {
            showMsg("没有找到匹配项!")
        }
        matchIndexesMap[keyword] = matchIndexes
    }

    // 查找下一个匹配项并滚动到该位置
    fun findNextMatchAndScroll(keyword: String) {
        val matchIndexes = matchIndexesMap[keyword]
        if (matchIndexes == null || matchIndexes.isEmpty()) {
            return
        }
        currentIndex = (currentIndex + 1) % matchIndexes.size // 循环到最后一个后回到第一个
        scrollToMatch(matchIndexes[currentIndex])
    }

    // 查找上一个匹配项并滚动到该位置
    fun findPreviousMatchAndScroll(keyword: String) {
        val matchIndexes = matchIndexesMap[keyword]
        if (matchIndexes == null || matchIndexes.isEmpty()) {
            return
        }
        currentIndex = (currentIndex - 1 + matchIndexes.size) % matchIndexes.size // 循环到第一个前回到最后一个
        scrollToMatch(matchIndexes[currentIndex])
    }

    // 滚动到指定匹配项
    private fun scrollToMatch(matchIndex: Int) {
        tv_results!!.post {
            val layout = tv_results!!.layout
            if (layout == null || tv_results!!.text.length <= matchIndex) return@post

            // 获取匹配项所在的行号
            val line = layout.getLineForOffset(matchIndex)

            // 获取匹配项所在行的顶部位置（相对于 TextView）
            val topOfLine = layout.getLineTop(line)
            val bottomOfLine = layout.getLineBottom(line)

            // 计算匹配项的垂直中心点（相对于 TextView）
            val verticalCenter = topOfLine + (bottomOfLine - topOfLine) / 2

            // 使用匹配项的垂直中心点作为新的滚动位置
            var newScrollY = verticalCenter

            // 确保新的滚动位置不会超出 ScrollView 的边界
            val totalHeight = layout.height // 文档总高度
            val scrollViewHeight = nsv_scroll!!.height // ScrollView 的可见高度
            val maxScrollY = max(0.0, (totalHeight - scrollViewHeight).toDouble())
                .toInt() // 最大滚动位置

            // 确保滚动位置在合法范围内
            newScrollY = min(newScrollY.toDouble(), maxScrollY.toDouble()).toInt()
            newScrollY = max(newScrollY.toDouble(), 0.0).toInt()
            showLog("Calculated Scroll Y: $newScrollY")
            // 使用 smoothScrollTo 来确保滚动是平滑的
            nsv_scroll!!.smoothScrollTo(0, newScrollY)
        }
    }

    // 高亮关键字
    private fun highlightKeyword(json: String, keyword: String): SpannableStringBuilder {
        val matchIndexes = matchIndexesMap[keyword]
        if (matchIndexes == null || matchIndexes.isEmpty()) {
            return SpannableStringBuilder(json)
        }
        val spannableJson = SpannableStringBuilder(json)
        // 创建新的 ForegroundColorSpan 实例用于每个匹配项
        for (matchIndex in matchIndexes) {
            val colorSpan = ForegroundColorSpan(Color.RED)
            spannableJson.setSpan(
                colorSpan,
                matchIndex,
                matchIndex + keyword.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableJson
    }

    // 在初始化界面或用户输入新的关键词时调用
    fun onSearch(keyword: String) {
        val result = tv_results!!.text.toString()
        if (!TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(result)) {
            buildMatchIndexes(result, keyword)
            val highlightedJson = highlightKeyword(result, keyword)
            tv_results!!.text = highlightedJson // 直接设置 SpannableStringBuilder 给 TextView
        } else {
            tv_results!!.text = result // 如果没有关键词则不进行高亮
        }
    }

    private fun closeContentView() {
        if (!mCanClose || mContainView!!.visibility == INVISIBLE) {
            return
        }
        mCanClose = false
        loadAnimOut(logViewMain, object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                mContainView!!.visibility = INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
    }

    /**
     * 往左进入
     *
     * @param view
     */
    private fun slideInFromRight(view: View) {
        val oa = ObjectAnimator.ofFloat(view, "translationX", view.width.toFloat(), 0f)
        oa.setDuration(300)
        oa.start()
        oa.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                bt_back!!.visibility = VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
    }

    /**
     * 往左退出
     */
    private fun slideOutFromRight(view: View?) {
        val oa = ObjectAnimator.ofFloat(view, "translationX", 0f, view!!.width.toFloat())
        oa.setDuration(300)
        oa.start()
        oa.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                bt_back!!.visibility = INVISIBLE
                ll_search!!.visibility = INVISIBLE
                tv_clear!!.visibility = VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
    }

    /**
     * 往右退出
     */
    private fun slideOutFromLeft(view: View) {
        val oa = ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat())
        oa.setDuration(300)
        oa.start()
    }

    /**
     * 往右进入
     */
    private fun slideInFromLeft(view: View?) {
        val oa = ObjectAnimator.ofFloat(view, "translationX", -view!!.width.toFloat(), 0f)
        oa.setDuration(300)
        oa.start()
    }

    private fun loadAnimIn(contentView: View?) {
        mCanClose = true
        val animation: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            -1f,
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.ABSOLUTE,
            0f,
            Animation.ABSOLUTE,
            0f
        )
        animation.duration = 250
        contentView!!.startAnimation(animation)
    }

    private fun loadAnimOut(contentView: View?, listener: Animation.AnimationListener) {
        val animation: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            -1f,
            Animation.ABSOLUTE,
            0f,
            Animation.ABSOLUTE,
            0f
        )
        animation.duration = 250
        animation.setAnimationListener(listener)
        contentView!!.startAnimation(animation)
    }

    fun createParams(): WindowManager.LayoutParams {
        val newParams = WindowManager.LayoutParams()
        // 设置window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            newParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            newParams.type = WindowManager.LayoutParams.TYPE_TOAST
        }
        // 设置图片格式，效果为背景透明
        newParams.format = PixelFormat.RGBA_8888
        val flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        newParams.flags = flags
        //      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
//      WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
//      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
        // 调整悬浮窗显示的停靠位置为左侧置
        newParams.gravity = Gravity.LEFT or Gravity.TOP
        newParams.x = 0
        newParams.y = 0
        newParams.width = LayoutParams.MATCH_PARENT
        newParams.height = LayoutParams.MATCH_PARENT
        return newParams
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // 获取相对屏幕的坐标，即以屏幕左上角为原点
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()
        if (!mCanHide) {
            restoreView()
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchStartX = event.x
                mTouchStartY = event.y
                mDraging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val mMoveStartX = event.x
                val mMoveStartY = event.y
                // 如果移动量大于3才移动
                if (abs((mTouchStartX - mMoveStartX).toDouble()) > 3
                    && abs((mTouchStartY - mMoveStartY).toDouble()) > 3
                ) {
                    mDraging = true
                }
                // 更新浮动窗口位置参数
                mWmParams!!.x = (x - mTouchStartX).toInt()
                mWmParams!!.y = (y - mTouchStartY).toInt()
                mWindowManager!!.updateViewLayout(this, mWmParams)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                updatePosition(mWmParams!!.x)
                timerForHide()
                run {
                    mTouchStartY = 0f
                    mTouchStartX = mTouchStartY
                }
            }
        }
        return false
    }

    private fun restoreView() {
        translateX(0f, 100)
        mCanHide = true
    }

    /**
     * 在X轴进行平移 左右
     */
    private fun translateX(di: Float, duration: Long) {
        if (translateX == null) {
            translateX = ObjectAnimator.ofFloat(this, "translationX", 0f, di)
        }
        translateX!!.setFloatValues(0f, di)
        translateX!!.setDuration(duration)
        translateX!!.interpolator = LinearInterpolator()
        translateX!!.start()
        translateX!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
        TRANSLATE_TOLEFT = !TRANSLATE_TOLEFT
    }

    private fun removeFloatView() {
        try {
            if (mContainView != null) {
                mContainView!!.removeAllViews()
                mWindowManager!!.removeViewImmediate(mContainView)
                mContainView = null
            }
            mWindowManager!!.removeView(this)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * 显示悬浮窗
     */
    fun show() {
        mWmParams?.y = (mScreenHeight / 4).toInt()
        updatePosition(mWmParams!!.x)
        timerForHide()
    }

    /**
     * 定时隐藏float view
     */
    private fun timerForHide() {
        mCanHide = true
        // 清除上一次的定时任务
        mHandler?.removeCallbacksAndMessages(null)
        // 进行定时操作，延时1秒后隐藏悬浮窗
        mHandler?.postDelayed({
            if (mCanHide) {
                mHandler?.sendEmptyMessage(HANDLER_TYPE_HIDE_LOGO)
            }
        }, 1000)
    }

    /**
     * 是否Float view
     */
    fun destroy() {
        showLog("销毁了.......")
        removeFloatView()
        mHandler?.removeCallbacksAndMessages(null)
    }

    fun setContent(event: HttpLogEvent) {
        logViewMain!!.post {
            if (mAdapter != null) {
                val data = mAdapter!!.getData()
                if (data.contains(event)) {
                    for (logEvent in data) {
                        if (event == logEvent) {
                            logEvent.results = event.results
                        }
                    }
                } else {
                    mAdapter!!.addData(event)
                }
            }
        }
    }
}

