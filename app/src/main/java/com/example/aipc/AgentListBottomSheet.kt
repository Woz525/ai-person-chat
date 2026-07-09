package com.example.aipc

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout

class AgentListBottomSheet(
    private val onAgentSelected: (Agent) -> Unit,
    private val onAgentDeleted: (String) -> Unit,
    private val onWorldSelected: ((String) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private var currentTab = 0
    private var agentAdapter: AgentAdapter? = null
    private var worldAdapter: WorldAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_agent_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.rvAgentList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                refreshList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        refreshList()
    }

    private fun refreshList() {
        if (currentTab == 0) showAgents() else showWorlds()
    }

    private fun showAgents() {
        val agents = FileManager.getAllAgents(requireContext())
        agentAdapter = AgentAdapter(agents) { agent, action ->
            when (action) {
                "switch" -> {
                    onAgentSelected(agent)
                    dismiss()
                }
                "edit" -> Toast.makeText(requireContext(), "编辑预留: ${agent.name}", Toast.LENGTH_SHORT).show()
                "delete" -> {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("删除智能体")
                        .setMessage("确定要删除“${agent.name}”吗？此操作不可恢复。")
                        .setPositiveButton("删除") { _, _ ->
                            FileManager.deleteAgent(requireContext(), agent.id)
                            onAgentDeleted(agent.id)
                            val newList = FileManager.getAllAgents(requireContext())
                            agentAdapter?.updateList(newList)
                            Toast.makeText(requireContext(), "已删除 ${agent.name}", Toast.LENGTH_SHORT).show()
                            if (newList.isEmpty()) dismiss()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }
        recyclerView.adapter = agentAdapter
    }

    // ========== 修改点：世界组改为显示红色提示文字 ==========
    private fun showWorlds() {
        val worlds = listOf(
            World("暂未开发群聊/世界组功能", "功能开发中")
        )
        worldAdapter = WorldAdapter(worlds) { world ->
            Toast.makeText(requireContext(), "世界组功能暂未开放", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        recyclerView.adapter = worldAdapter
    }
    // ===================================================

    // ==================== 智能体适配器（完整保留，未改动） ====================
    inner class AgentAdapter(
        private var agents: List<Agent>,
        private val onAction: (Agent, String) -> Unit
    ) : RecyclerView.Adapter<AgentAdapter.ViewHolder>() {

        fun updateList(newList: List<Agent>) {
            agents = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            // 根布局
            val root = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                background = ContextCompat.getDrawable(context, typedValue.resourceId)
            }

            // 头像
            val avatarCard = CardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
                radius = dpToPx(24).toFloat()
                elevation = dpToPx(2).toFloat()
            }
            val avatarImage = ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            avatarCard.addView(avatarImage)

            // 名字
            val nameText = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(12)
                    marginEnd = dpToPx(12)
                }
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            // 按钮容器
            val buttonContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // 创建正方形按钮
            fun createSquareButton(iconRes: Int, text: String, color: Int): Button {
                val buttonSize = dpToPx(48) // 正方形边长 48dp
                return Button(context).apply {
                    layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                        marginStart = dpToPx(4)
                    }
                    this.text = text
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 11f
                    setPadding(0, dpToPx(4), 0, dpToPx(2))
                    // 圆角背景
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(color)
                        cornerRadius = dpToPx(8).toFloat()
                    }
                    background = drawable
                    // 图标在上方
                    val icon = ContextCompat.getDrawable(context, iconRes)
                    icon?.setBounds(0, 0, dpToPx(20), dpToPx(20))
                    setCompoundDrawables(null, icon, null, null)
                    compoundDrawablePadding = dpToPx(4)
                    gravity = Gravity.CENTER_HORIZONTAL
                    // 确保文字和图标不超出按钮范围
                    includeFontPadding = false
                }
            }

            val editBtn = createSquareButton(R.drawable.ic_edit, "编辑", 0xFF4CAF50.toInt())
            val switchBtn = createSquareButton(R.drawable.ic_switch, "切换", 0xFF2196F3.toInt())
            val deleteBtn = createSquareButton(R.drawable.ic_delete, "删除", 0xFFF44336.toInt())

            buttonContainer.addView(editBtn)
            buttonContainer.addView(switchBtn)
            buttonContainer.addView(deleteBtn)

            root.addView(avatarCard)
            root.addView(nameText)
            root.addView(buttonContainer)

            return ViewHolder(root, avatarImage, nameText, editBtn, switchBtn, deleteBtn)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val agent = agents[position]
            holder.nameText.text = agent.name
            val bitmap = Agent.base64ToBitmap(agent.avatarBase64)
            Glide.with(holder.itemView.context).load(bitmap).circleCrop().into(holder.avatarImage)

            holder.editBtn.setOnClickListener { onAction(agent, "edit") }
            holder.switchBtn.setOnClickListener { onAction(agent, "switch") }
            holder.deleteBtn.setOnClickListener { onAction(agent, "delete") }
        }

        override fun getItemCount(): Int = agents.size

        inner class ViewHolder(
            itemView: View,
            val avatarImage: ImageView,
            val nameText: TextView,
            val editBtn: Button,
            val switchBtn: Button,
            val deleteBtn: Button
        ) : RecyclerView.ViewHolder(itemView)
    }

    // ==================== 世界组适配器（修改：文字颜色为红色） ====================
    inner class WorldAdapter(
        private val worlds: List<World>,
        private val onEnter: (World) -> Unit
    ) : RecyclerView.Adapter<WorldAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_world, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val world = worlds[position]
            holder.tvName.text = world.name
            holder.tvName.setTextColor(android.graphics.Color.RED)  // 红色文字
            holder.btnEnter.setOnClickListener { onEnter(world) }
        }

        override fun getItemCount(): Int = worlds.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvWorldName)
            val btnEnter: Button = itemView.findViewById(R.id.btnEnterWorld)
        }
    }

    data class World(val name: String, val description: String)

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}