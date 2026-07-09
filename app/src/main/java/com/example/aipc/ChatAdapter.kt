package com.example.aipc

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatAdapter(
    private val messages: MutableList<Message>,
    private var userAvatarPath: String = "",
    private var userName: String = "用户",
    private var botAvatarBase64: String = "",
    private var botName: String = "智能体"
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private val loadingStates = mutableMapOf<String, LoadingState>()
    private val typingStates = mutableMapOf<String, TypingState>()

    data class LoadingState(var dotCount: Int, var runnable: Runnable?)
    data class TypingState(var index: Int, var runnable: Runnable?)

    // 更新智能体信息（切换智能体时调用）
    fun updateBotInfo(avatarBase64: String, name: String) {
        botAvatarBase64 = avatarBase64
        botName = name
        notifyDataSetChanged()
    }

    fun updateUserInfo(avatarPath: String, name: String) {
        userAvatarPath = avatarPath
        userName = name
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isLoading) 2 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        // 清除之前动画
        stopLoadingAnimation(message.id)
        stopTypingAnimation(message.id)

        if (message.isLoading) {
            holder.bindLoading(botAvatarBase64, botName)
            startLoadingAnimation(holder, message.id)
        } else {
            holder.bind(message, userAvatarPath, userName, botAvatarBase64, botName)
            if (!message.isUser && !message.isNarration && message.isTyping) {
                val state = typingStates[message.id]
                if (state != null && state.index < message.content.length) {
                    resumeTyping(holder, message.id, message.content, state.index, botName)
                } else {
                    startTypingEffect(holder, message.id, message.content, botName)
                }
            } else {
                holder.showFullText()
            }
        }
    }

    // 加载动画相关（省略，与之前相同，需将绑定方法中的bot参数传入）
    private fun startLoadingAnimation(holder: MessageViewHolder, msgId: String) {
        if (loadingStates.containsKey(msgId)) return
        var dotCount = 0
        val runnable = object : Runnable {
            override fun run() {
                val dots = when (dotCount % 3) {
                    0 -> "."
                    1 -> ".."
                    else -> "..."
                }
                holder.setLoadingText(dots)
                dotCount++
                loadingStates[msgId]?.dotCount = dotCount
                handler.postDelayed(this, 500)
            }
        }
        loadingStates[msgId] = LoadingState(0, runnable)
        handler.post(runnable)
    }

    private fun stopLoadingAnimation(msgId: String) {
        loadingStates[msgId]?.runnable?.let { handler.removeCallbacks(it) }
    }

    private fun clearLoadingState(msgId: String) {
        loadingStates[msgId]?.runnable?.let { handler.removeCallbacks(it) }
        loadingStates.remove(msgId)
    }

    // 打字机效果
    private fun startTypingEffect(holder: MessageViewHolder, msgId: String, fullText: String, botName: String) {
        holder.clearText()
        var index = 0
        val chunkSize = 2
        val runnable = object : Runnable {
            override fun run() {
                if (index < fullText.length) {
                    val end = minOf(index + chunkSize, fullText.length)
                    holder.appendText(fullText.substring(index, end))
                    index = end
                    val state = typingStates[msgId]
                    if (state != null) {
                        state.index = index
                        handler.postDelayed(this, 40)
                    }
                } else {
                    typingStates.remove(msgId)
                    val msgIndex = messages.indexOfFirst { it.id == msgId }
                    if (msgIndex != -1) {
                        val msg = messages[msgIndex]
                        if (msg.isTyping) {
                            messages[msgIndex] = msg.copy(isTyping = false)
                            notifyItemChanged(msgIndex)
                        }
                    }
                }
            }
        }
        typingStates[msgId] = TypingState(0, runnable)
        handler.post(runnable)
    }

    private fun resumeTyping(holder: MessageViewHolder, msgId: String, fullText: String, startIndex: Int, botName: String) {
        val state = typingStates[msgId] ?: return
        holder.clearText()
        if (startIndex > 0) {
            holder.appendText(fullText.substring(0, startIndex))
        }
        var index = startIndex
        val chunkSize = 2
        val runnable = object : Runnable {
            override fun run() {
                if (index < fullText.length) {
                    val end = minOf(index + chunkSize, fullText.length)
                    holder.appendText(fullText.substring(index, end))
                    index = end
                    val currentState = typingStates[msgId]
                    if (currentState != null) {
                        currentState.index = index
                        handler.postDelayed(this, 40)
                    }
                } else {
                    typingStates.remove(msgId)
                    val msgIndex = messages.indexOfFirst { it.id == msgId }
                    if (msgIndex != -1) {
                        val msg = messages[msgIndex]
                        if (msg.isTyping) {
                            messages[msgIndex] = msg.copy(isTyping = false)
                            notifyItemChanged(msgIndex)
                        }
                    }
                }
            }
        }
        state.runnable = runnable
        handler.post(runnable)
    }

    private fun stopTypingAnimation(msgId: String) {
        typingStates[msgId]?.runnable?.let { handler.removeCallbacks(it) }
        typingStates.remove(msgId)
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun replaceLoadingMessage(loadingId: String, newMessage: Message) {
        val index = messages.indexOfFirst { it.id == loadingId && it.isLoading }
        if (index != -1) {
            clearLoadingState(loadingId)
            messages[index] = newMessage
            notifyItemChanged(index)
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llAgentInfo: View = itemView.findViewById(R.id.llAgentInfo)
        private val ivAgentInfoAvatar: ImageView = itemView.findViewById(R.id.ivAgentInfoAvatar)
        private val tvAgentInfoName: TextView = itemView.findViewById(R.id.tvAgentInfoName)
        private val tvMessageUser: TextView = itemView.findViewById(R.id.tvMessageUser)
        private val tvMessageBot: TextView = itemView.findViewById(R.id.tvMessageBot)
        private val tvNarration: TextView = itemView.findViewById(R.id.tvNarration)
        private val llUser: View = itemView.findViewById(R.id.llUser)
        private val llBot: View = itemView.findViewById(R.id.llBot)
        private val llNarration: View = itemView.findViewById(R.id.llNarration)
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val ivBotAvatar: ImageView = itemView.findViewById(R.id.ivBotAvatar)
        private val tvUserNameView: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvBotNameView: TextView = itemView.findViewById(R.id.tvBotName)

        fun bindLoading(botAvatarBase64: String, botName: String) {
            // 重置所有容器
            llUser.visibility = View.GONE
            llBot.visibility = View.VISIBLE
            llNarration.visibility = View.GONE
            llAgentInfo.visibility = View.GONE

            // 确保 item 可见且高度正常
            itemView.visibility = View.VISIBLE
            val lp = itemView.layoutParams
            if (lp != null) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                itemView.layoutParams = lp
            }

            tvMessageBot.text = "."
            tvBotNameView.text = botName
            if (botAvatarBase64.isNotEmpty()) {
                val bitmap = Agent.base64ToBitmap(botAvatarBase64)
                Glide.with(itemView.context).load(bitmap).circleCrop().into(ivBotAvatar)
            }
        }

        fun setLoadingText(dots: String) {
            tvMessageBot.text = dots
        }

        fun bind(message: Message, userAvatarPath: String, userName: String, botAvatarBase64: String, botName: String) {
            // 先重置所有容器可见性
            llUser.visibility = View.GONE
            llBot.visibility = View.GONE
            llNarration.visibility = View.GONE
            llAgentInfo.visibility = View.GONE

            when {
                message.isAgentInfo -> {
                    // ★ 卡片分支
                    llAgentInfo.visibility = View.VISIBLE
                    tvAgentInfoName.text = botName
                    if (botAvatarBase64.isNotEmpty()) {
                        val bitmap = Agent.base64ToBitmap(botAvatarBase64)
                        Glide.with(itemView.context).load(bitmap).circleCrop()
                            .into(ivAgentInfoAvatar)
                    }

                    // ★★★ 根据 cardVisible 控制 item 显示/隐藏 ★★★
                    if (message.cardVisible) {
                        itemView.visibility = View.VISIBLE
                        // 恢复高度为 WRAP_CONTENT
                        val lp = itemView.layoutParams
                        if (lp != null) {
                            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                            itemView.layoutParams = lp
                        }
                    } else {
                        itemView.visibility = View.GONE
                        // 高度设为 0 消除占位
                        val lp = itemView.layoutParams
                        if (lp != null) {
                            lp.height = 0
                            itemView.layoutParams = lp
                        }
                    }
                    itemView.requestLayout()
                }

                message.isNarration -> {
                    // 旁白
                    itemView.visibility = View.VISIBLE
                    val lp = itemView.layoutParams
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        itemView.layoutParams = lp
                    }
                    llNarration.visibility = View.VISIBLE
                    tvNarration.text = message.content
                }

                message.isUser -> {
                    // 用户消息
                    itemView.visibility = View.VISIBLE
                    val lp = itemView.layoutParams
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        itemView.layoutParams = lp
                    }
                    llUser.visibility = View.VISIBLE
                    tvMessageUser.text = message.content
                    tvUserNameView.text = userName
                    if (userAvatarPath.isNotEmpty()) {
                        Glide.with(itemView.context).load(userAvatarPath).circleCrop()
                            .into(ivUserAvatar)
                    }
                }

                else -> {
                    // 智能体普通消息（包括加载状态等，但加载状态单独在 bindLoading 处理）
                    itemView.visibility = View.VISIBLE
                    val lp = itemView.layoutParams
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        itemView.layoutParams = lp
                    }
                    llBot.visibility = View.VISIBLE
                    tvMessageBot.text = message.content
                    tvBotNameView.text = botName
                    if (botAvatarBase64.isNotEmpty()) {
                        val bitmap = Agent.base64ToBitmap(botAvatarBase64)
                        Glide.with(itemView.context).load(bitmap).circleCrop().into(ivBotAvatar)
                    }
                }
            }
        }

        fun clearText() {
            tvMessageBot.text = ""
        }

        fun appendText(text: String) {
            tvMessageBot.append(text)
        }

        fun showFullText() {
            // 直接显示完整文本，由于 bind 已设置，无需额外操作
            // 但确保 bot 名称正确
        }
    }
}