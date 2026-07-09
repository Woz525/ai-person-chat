package com.example.aipc

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationViewLeft: NavigationView
    private lateinit var navigationViewRight: NavigationView
    private lateinit var dataManager: DataManager

    // 模式控件
    private lateinit var rgMode: RadioGroup
    private lateinit var rbModePure: RadioButton
    private lateinit var rbModeSlightlyYellow: RadioButton
    private lateinit var rbModeYellow: RadioButton
    private lateinit var rbModeBloody: RadioButton
    private lateinit var rbModeFamily: RadioButton
    private var isProgrammaticChange = false

    // 锁死控件
    private lateinit var llLockOption: LinearLayout
    private lateinit var rgLock: RadioGroup
    private lateinit var rbLockYes: RadioButton
    private lateinit var rbLockNo: RadioButton

    private lateinit var etContextLimit: EditText
    private var homeFragment: HomeFragment? = null

    // ★★★ 新增：当前智能体 ID ★★★
    private var currentAgentId: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aipc_prefs", MODE_PRIVATE)
        val hasAgreed = prefs.getBoolean("disclaimer_agreed", false)

        if (!hasAgreed) {
            showDisclaimerDialog()
            return
        }

        initMainContent()
    }

    private fun showDisclaimerDialog() {
        AlertDialog.Builder(this)
            .setTitle("【法律声明】用户协议及免责条款")
            .setMessage(
                "欢迎使用 AIPC（以下简称“本软件”）。\n\n" +
                        "一、服务性质\n" +
                        "本软件官方内置的API服务（智谱AI）严格遵守中国法律法规，不提供任何色情、血腥、暴力或违规内容。\n\n" +
                        "二、【高风险功能】自定义API\n" +
                        "本软件提供“自定义API”功能，允许您连接您自己或第三方的API服务。您必须明确：\n" +
                        "1. 您使用该功能所连接的任何服务，其内容、合法性、安全性均由您自行完全负责；\n" +
                        "2. 您可能通过该功能接触到包括但不限于：色情、性暗示、血腥、暴力、仇恨言论、违反中国法律的内容；\n" +
                        "3. 本软件开发者无法、也不对自定义API中的任何内容进行事先审核或控制。\n\n" +
                        "三、用户承诺与责任\n" +
                        "您承诺：\n" +
                        "1. 您不会利用本软件制作、复制、发布、传播任何违反中华人民共和国法律法规的信息；\n" +
                        "2. 您使用自定义API的行为完全出于合法目的，且您已获得所连接API的合法授权；\n" +
                        "3. 您将对因使用自定义API而产生的所有后果（包括但不限于法律诉讼、行政处罚、民事赔偿）承担全部责任。\n\n" +
                        "四、免责声明\n" +
                        "本软件开发者（以下简称“我”）在此声明：\n" +
                        "1. 我不承担因您使用自定义API而导致的任何直接或间接损失；\n" +
                        "2. 我不承担因您违反法律法规使用本软件而产生的任何责任；\n" +
                        "3. 我不对任何第三方API的可用性、准确性、安全性提供任何担保。\n\n" +
                        "五、法律适用与争议解决\n" +
                        "本协议适用中华人民共和国法律。因本协议产生的任何争议，应提交开发者所在地有管辖权的人民法院诉讼解决。\n\n" +
                        "⚠️ 请您仔细阅读以上条款。您点击“同意”即表示您已完全理解并接受本协议的全部内容。如不同意，请立即退出。"
            )
            .setPositiveButton("我已阅读并同意") { _, _ ->
                getSharedPreferences("aipc_prefs", MODE_PRIVATE).edit()
                    .putBoolean("disclaimer_agreed", true).apply()
                initMainContent()
            }
            .setNegativeButton("不同意并退出") { _, _ ->
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    private fun initMainContent() {
        dataManager = DataManager(this)
        val language = dataManager.getLanguage()
        LocaleHelper.updateResources(this, language)

        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        navigationViewLeft = findViewById(R.id.nav_view_left)
        navigationViewRight = findViewById(R.id.nav_view_right)

        setupDrawerLayout()
        setupBottomNavigation()
        setupRightDrawer()
        updateLeftDrawer()
        applyFamilyModeLock()
    }

    // 设置当前智能体 ID（由 HomeFragment 调用）
    fun setCurrentAgentId(agentId: String) {
        currentAgentId = agentId
        updateLeftDrawer()
        val homeFragment = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
        homeFragment?.let {
            updateBgMusicName(it.getCurrentBgMusicName())
        }
    }

    private fun setupDrawerLayout() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, null,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navigationViewLeft.getHeaderView(0)
        llLockOption = headerView.findViewById(R.id.llLockOption)
        rgLock = headerView.findViewById(R.id.rgLock)
        rbLockYes = headerView.findViewById(R.id.rbLockYes)
        rbLockNo = headerView.findViewById(R.id.rbLockNo)

        if (dataManager.getLockFavorability(currentAgentId)) {
            rbLockYes.isChecked = true
        } else {
            rbLockNo.isChecked = true
        }

        rgLock.setOnCheckedChangeListener { _, checkedId ->
            val lock = checkedId == R.id.rbLockYes
            dataManager.setLockFavorability(currentAgentId, lock)
            updateLeftDrawer()
        }

        etContextLimit = headerView.findViewById(R.id.etContextLimit)
        etContextLimit.setText(dataManager.getContextLimit().toString())
        etContextLimit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = etContextLimit.text.toString()
                val limit = text.toIntOrNull() ?: 8
                dataManager.setContextLimit(limit)
                Toast.makeText(this, "上下文限制已更新为 $limit", Toast.LENGTH_SHORT).show()
            }
        }

        updateLockOptionVisibility()

        navigationViewLeft.setNavigationItemSelectedListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateLockOptionVisibility() {
        val storyType = dataManager.getStoryType()
        val mode = dataManager.getMode()
        if (storyType == "为所欲为" && mode != "亲情") {
            llLockOption.visibility = View.VISIBLE
        } else {
            llLockOption.visibility = View.GONE
        }
    }

    private fun applyFamilyModeLock() {
        val mode = dataManager.getMode()
        if (mode == "亲情" && currentAgentId.isNotEmpty()) {
            dataManager.setLockFavorability(currentAgentId, true)
            updateLeftDrawer()
            llLockOption.visibility = View.GONE
        }
    }
    fun updateBgMusicName(name: String) {
        val headerView = navigationViewRight.getHeaderView(0)
        val tvBgMusicName = headerView.findViewById<TextView>(R.id.tvBgMusicName)
        tvBgMusicName.text = if (name.isNotEmpty()) name else "(未导入)"
    }

    private fun setupRightDrawer() {
        val headerView = navigationViewRight.getHeaderView(0)
        val spLanguage = headerView.findViewById<Spinner>(R.id.spLanguage)
        val etName = headerView.findViewById<EditText>(R.id.etUserName)
        val rgGender = headerView.findViewById<RadioGroup>(R.id.rgUserGender)
        rgMode = headerView.findViewById(R.id.rgMode)
        rbModePure = headerView.findViewById(R.id.rbModePure)
        rbModeSlightlyYellow = headerView.findViewById(R.id.rbModeSlightlyYellow)
        rbModeYellow = headerView.findViewById(R.id.rbModeYellow)
        rbModeBloody = headerView.findViewById(R.id.rbModeBloody)
        rbModeFamily = headerView.findViewById(R.id.rbModeFamily)
        val rgStory = headerView.findViewById<RadioGroup>(R.id.rgStoryType)
        val btnEditAgent = headerView.findViewById<Button>(R.id.btnEditAgent)

        // ★★★ UI样式切换 ★★★
        val rgUiMode = headerView.findViewById<RadioGroup>(R.id.rgUiMode)
        val llBackgroundSource = headerView.findViewById<LinearLayout>(R.id.llBackgroundSourceGroup)  // 注意ID
        val rgBackgroundSource = headerView.findViewById<RadioGroup>(R.id.rgBackgroundSource)
        val btnUploadBackground = headerView.findViewById<Button>(R.id.btnUploadBackground)
        val btnRegenerateAI = headerView.findViewById<Button>(R.id.btnRegenerateAI)

        // 获取 HomeFragment 实例
        val homeFragment = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment

        // 初始化 UI 模式（先设置 RadioButton 状态）
        val currentUiMode = dataManager.getUiMode()
        if (currentUiMode == "classic") {
            rgUiMode.check(R.id.rbNoBackground)
            llBackgroundSource.visibility = View.GONE
        } else {
            rgUiMode.check(R.id.rbWithBackground)
            llBackgroundSource.visibility = View.VISIBLE
        }

        // 初始化背景来源 RadioButton 状态（需要当前智能体 ID）
        if (homeFragment != null) {
            val currentAgentId = homeFragment.getCurrentAgentId()
            if (currentAgentId.isNotEmpty()) {
                val currentSource = dataManager.getBackgroundSource(currentAgentId)
                if (currentSource == "ai") {
                    rgBackgroundSource.check(R.id.rbBackgroundAI)
                    btnUploadBackground.visibility = View.GONE
                } else {
                    rgBackgroundSource.check(R.id.rbBackgroundManual)
                    btnUploadBackground.visibility = View.VISIBLE
                }
            } else {
                rgBackgroundSource.check(R.id.rbBackgroundAI)
                btnUploadBackground.visibility = View.GONE
            }
        } else {
            rgBackgroundSource.check(R.id.rbBackgroundAI)
            btnUploadBackground.visibility = View.GONE
        }

        // UI 样式切换监听
        rgUiMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rbNoBackground) "classic" else "modern"
            dataManager.setUiMode(mode)
            // 控制背景来源分组的显示
            if (mode == "classic") {
                llBackgroundSource.visibility = View.GONE
            } else {
                llBackgroundSource.visibility = View.VISIBLE
            }
            // 通知 HomeFragment 切换模式
            val homeFrag = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
            homeFrag?.setUiMode(mode)
        }

        // 背景来源切换监听
        rgBackgroundSource.setOnCheckedChangeListener { _, checkedId ->
            val source = if (checkedId == R.id.rbBackgroundAI) "ai" else "manual"
            val homeFrag = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
            homeFrag?.setBackgroundSource(source)
            // 控制按钮显示
            if (source == "manual") {
                btnUploadBackground.visibility = View.VISIBLE
                btnRegenerateAI.visibility = View.GONE
            } else {
                btnUploadBackground.visibility = View.GONE
                btnRegenerateAI.visibility = View.VISIBLE
            }
        }
        btnRegenerateAI.setOnClickListener {
            val homeFrag = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
            homeFrag?.showRegenerateDialog()
        }

        // 背景音乐
        val btnImportBgMusic = headerView.findViewById<Button>(R.id.btnImportBgMusic)
        val tvBgMusicName = headerView.findViewById<TextView>(R.id.tvBgMusicName)


// 导入音频按钮
        // 在 setupRightDrawer 中
        btnImportBgMusic.setOnClickListener {
            this.homeFragment?.importBgMusic()
                ?: Toast.makeText(this, "HomeFragment 未初始化", Toast.LENGTH_SHORT).show()
        }


        // 上传按钮点击事件
        btnUploadBackground.setOnClickListener {
            val homeFrag = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
            homeFrag?.selectManualBackground()
        }

        // ========== 以下为原有设置代码（语言、名字、性别等） ==========
        etName.setText(dataManager.getUserName())
        when (dataManager.getUserGender()) {
            "男" -> rgGender.check(R.id.rbMale)
            "女" -> rgGender.check(R.id.rbFemale)
            else -> rgGender.check(R.id.rbSecret)
        }
        val languagePos = if (dataManager.getLanguage() == "Chinese") 0 else 1
        spLanguage.setSelection(languagePos)

        val currentMode = dataManager.getMode()
        when (currentMode) {
            "纯净" -> rbModePure.isChecked = true
            "微黄" -> rbModeSlightlyYellow.isChecked = true
            "黄" -> rbModeYellow.isChecked = true
            "血腥" -> rbModeBloody.isChecked = true
            "亲情" -> rbModeFamily.isChecked = true
        }

        val storyPos = if (dataManager.getStoryType() == "真实") 0 else 1
        rgStory.check(if (storyPos == 0) R.id.rbReal else R.id.rbArbitrary)

        val rbReal = headerView.findViewById<RadioButton>(R.id.rbReal)
        rbReal.isEnabled = false

        updateModeRadioGroupState()

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            if (isProgrammaticChange) return@setOnCheckedChangeListener
            val selectedMode = when (checkedId) {
                R.id.rbModePure -> "纯净"
                R.id.rbModeSlightlyYellow -> "微黄"
                R.id.rbModeYellow -> "黄"
                R.id.rbModeBloody -> "血腥"
                R.id.rbModeFamily -> "亲情"
                else -> "纯净"
            }
            val isSensitive = selectedMode == "微黄" || selectedMode == "黄" || selectedMode == "血腥"
            if (dataManager.getApiType() == "official" && isSensitive) {
                Toast.makeText(this, "官方API不支持该模式，请切换至自定义API", Toast.LENGTH_SHORT).show()
                restorePreviousMode()
                return@setOnCheckedChangeListener
            }
            if (isSensitive) {
                AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("“${selectedMode}”模式可能包含不适宜内容，是否继续？")
                    .setPositiveButton("确定") { _, _ -> dataManager.setMode(selectedMode) }
                    .setNegativeButton("取消") { _, _ -> restorePreviousMode() }
                    .setOnCancelListener { restorePreviousMode() }
                    .show()
            } else {
                dataManager.setMode(selectedMode)
                applyFamilyModeLock()
                updateLockOptionVisibility()
                updateLeftDrawer()
            }
        }

        spLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = if (position == 0) "Chinese" else "English"
                if (lang != dataManager.getLanguage()) {
                    dataManager.setLanguage(lang)
                    LocaleHelper.updateResources(this@MainActivity, lang)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etName.setOnFocusChangeListener { _, _ -> dataManager.setUserName(etName.text.toString()) }

        rgGender.setOnCheckedChangeListener { _, checkedId ->
            val gender = when (checkedId) {
                R.id.rbMale -> "男"
                R.id.rbFemale -> "女"
                else -> "保密"
            }
            dataManager.setUserGender(gender)
        }

        rgStory.setOnCheckedChangeListener { _, checkedId ->
            val story = if (checkedId == R.id.rbReal) "真实" else "为所欲为"
            dataManager.setStoryType(story)
            updateLeftDrawer()
            updateLockOptionVisibility()
            applyFamilyModeLock()
        }


        // 导入音频按钮
        // ★ 右上角菜单“导入背景音乐”按钮
        btnImportBgMusic.setOnClickListener {
            // ViewPager2 中 HomeFragment 在位置 1，默认 tag 为 "f1"
            val homeFrag = supportFragmentManager.findFragmentByTag("f1") as? HomeFragment
            if (homeFrag != null) {
                homeFrag.importBgMusic()
            } else {
                Toast.makeText(this, "请先切换到主页", Toast.LENGTH_SHORT).show()
            }
        }

        // 更新文件名显示（在加载智能体时调用）
        // 注意：这个方法不在这里定义，而是在 MainActivity 类中单独定义
        btnEditAgent.setOnClickListener {
            Toast.makeText(this, "编辑智能体功能预留", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePreviousMode() {
        val currentMode = dataManager.getMode()
        isProgrammaticChange = true
        when (currentMode) {
            "纯净" -> rbModePure.isChecked = true
            "微黄" -> rbModeSlightlyYellow.isChecked = true
            "黄" -> rbModeYellow.isChecked = true
            "血腥" -> rbModeBloody.isChecked = true
            "亲情" -> rbModeFamily.isChecked = true
        }
        isProgrammaticChange = false
    }

    fun updateModeRadioGroupState() {
        val isOfficial = dataManager.getApiType() == "official"
        rbModeSlightlyYellow.isEnabled = !isOfficial
        rbModeYellow.isEnabled = !isOfficial
        rbModeBloody.isEnabled = !isOfficial
        if (isOfficial) {
            val currentMode = dataManager.getMode()
            if (currentMode == "微黄" || currentMode == "黄" || currentMode == "血腥") {
                dataManager.setMode("纯净")
                isProgrammaticChange = true
                rbModePure.isChecked = true
                isProgrammaticChange = false
                Toast.makeText(this, "官方API下已自动切换至纯净模式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshModeState() {
        updateModeRadioGroupState()
    }

    // ★★★ 修改：无参，使用 currentAgentId ★★★
    fun updateLeftDrawer() {
        val headerView = navigationViewLeft.getHeaderView(0)
        val tvFav = headerView.findViewById<TextView>(R.id.tvFavorabilityValue)
        val tvLove = headerView.findViewById<TextView>(R.id.tvLoveValueValue)
        val tvKnown = headerView.findViewById<TextView>(R.id.tvKnownInfoValue)
        val tvMemory = headerView.findViewById<TextView>(R.id.tvKeyMemoriesValue)

        val storyType = dataManager.getStoryType()
        val mode = dataManager.getMode()
        // 如果 currentAgentId 为空，则显示占位符
        val agentId = currentAgentId
        if (agentId.isEmpty()) {
            tvFav.text = "—"
            tvLove.text = "—"
            tvKnown.text = "未开发完毕"
            tvMemory.text = "未开发完毕"
            return
        }
        val isLocked = dataManager.getLockFavorability(agentId)

        if (mode == "亲情" || (storyType == "为所欲为" && isLocked)) {
            tvFav.text = "∞"
            tvLove.text = "∞"
        } else {
            tvFav.text = dataManager.getFavorability(agentId).toString()
            tvLove.text = dataManager.getLoveValue(agentId).toString()
        }
        tvKnown.text = dataManager.getKnownInfo(agentId).ifEmpty { "未开发完毕" }
        tvMemory.text = dataManager.getKeyMemories(agentId).ifEmpty { "未开发完毕" }
    }

    fun openLeftDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else drawerLayout.openDrawer(GravityCompat.START)
    }

    fun openRightDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END)
        else drawerLayout.openDrawer(GravityCompat.END)
    }

    fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun setupBottomNavigation() {
        val fragments: List<Fragment> = listOf(ApiFragment(), HomeFragment(), CreateFragment(), ProfileFragment())
        homeFragment = fragments[1] as? HomeFragment  // ★ 保存 HomeFragment 实例
        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_api -> viewPager.currentItem = 0
                R.id.nav_home -> viewPager.currentItem = 1
                R.id.nav_create -> viewPager.currentItem = 2
                R.id.nav_profile -> viewPager.currentItem = 3
                else -> return@setOnNavigationItemSelectedListener false
            }
            true
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNavigationView.selectedItemId = R.id.nav_api
                    1 -> bottomNavigationView.selectedItemId = R.id.nav_home
                    2 -> bottomNavigationView.selectedItemId = R.id.nav_create
                    3 -> bottomNavigationView.selectedItemId = R.id.nav_profile
                }
            }
        })
    }
}