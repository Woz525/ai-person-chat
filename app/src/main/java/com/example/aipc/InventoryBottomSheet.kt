package com.example.aipc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InventoryBottomSheet(
    private val inventory: Inventory,
    private val onItemAction: (itemName: String, action: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvCapacity = view.findViewById<TextView>(R.id.tvInventoryCapacity)
        tvCapacity.text = "${inventory.getSize()} / ${inventory.getMaxSize()}"

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvInventory)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)
        recyclerView.adapter = InventoryAdapter(inventory.getItems(), onItemAction)
    }

    inner class InventoryAdapter(
        private val items: List<InventoryItem>,
        private val onItemAction: (itemName: String, action: String) -> Unit
    ) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.btnUse.setOnClickListener {
                onItemAction(item.name, "use")
                dismiss()
            }
            holder.btnDiscard.setOnClickListener {
                onItemAction(item.name, "discard")
                dismiss()
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvItemName)
            val btnUse: Button = itemView.findViewById(R.id.btnUse)
            val btnDiscard: Button = itemView.findViewById(R.id.btnDiscard)
        }
    }
}