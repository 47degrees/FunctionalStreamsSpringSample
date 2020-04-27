package com.fortyseven.degrees.streamingapp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fortyseven.degrees.streamingapp.R
import com.fortyseven.degrees.streamingapp.User
import kotlinx.android.synthetic.main.viewholder_user.view.*

// DiffUtil.ItemCallback is stateless thus can be object
object UserDiffUtil : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
        oldItem == newItem
}

val AsyncUserDiffUtil: AsyncDifferConfig<User> =
    AsyncDifferConfig.Builder(UserDiffUtil).build()

class UserAdapter : ListAdapter<User, UserViewHolder>(
    AsyncUserDiffUtil
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder =
        UserViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.viewholder_user,
                    parent,
                    false
                )
        )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) =
        holder.bind(getItem(position))
}

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(user: User): Unit {
        itemView.user_id.text = user.id.toString()
        itemView.user_name.text = user.name
    }
}