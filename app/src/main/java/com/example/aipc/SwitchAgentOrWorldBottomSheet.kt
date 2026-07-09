package com.example.aipc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout

class SwitchAgentOrWorldBottomSheet(
    private val onAgentSelected: (Agent) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_switch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 默认显示智能体列表
        showAgents()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showAgents()
                    1 -> showWorlds()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showAgents() {
        val agents = FileManager.getAllAgents(requireContext())
        val adapter = AgentListAdapter(agents) { agent ->
            onAgentSelected(agent)
            dismiss()
        }
        recyclerView.adapter = adapter
    }

    private fun showWorlds() {
        // 世界组功能预留，先显示占位
        val list = listOf("世界功能开发中...")
        val adapter = SimpleListAdapter(list) {
            Toast.makeText(requireContext(), "世界组功能预留", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        recyclerView.adapter = adapter
    }

    // 智能体列表适配器
    inner class AgentListAdapter(
        private val agents: List<Agent>,
        private val onItemClick: (Agent) -> Unit
    ) : RecyclerView.Adapter<AgentListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_agent_simple, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val agent = agents[position]
            holder.textName.text = agent.name
            val bitmap = Agent.base64ToBitmap(agent.avatarBase64)
            holder.imageAvatar.setImageBitmap(bitmap)
            holder.itemView.setOnClickListener { onItemClick(agent) }
        }

        override fun getItemCount(): Int = agents.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageAvatar: androidx.appcompat.widget.AppCompatImageView = itemView.findViewById(R.id.ivAvatar)
            val textName: TextView = itemView.findViewById(R.id.tvName)
        }
    }

    // 简单列表适配器（用于世界组）
    inner class SimpleListAdapter(
        private val items: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
            holder.itemView.setOnClickListener { onItemClick(items[position]) }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(android.R.id.text1)
        }
    }
}