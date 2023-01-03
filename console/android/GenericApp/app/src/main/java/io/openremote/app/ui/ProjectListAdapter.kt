package io.openremote.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.openremote.app.databinding.RowProjectItemBinding
import io.openremote.app.model.ProjectItem

class ProjectListAdapter(val items: MutableList<ProjectItem>, private val goToMainActivity: (url: String) -> (Unit)) : RecyclerView.Adapter<ProjectListAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RowProjectItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, goToMainActivity)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindProjectItem(items[position])
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return items.size
    }

    fun remove(position: Int) {
        this.items.removeAt(position)
        this.notifyDataSetChanged()
    }

    // Holds the views for adding it to image and text
    class ViewHolder(private val binding: RowProjectItemBinding, private val goToMainActivity: (url: String) -> (Unit)) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        lateinit var url: String

        init {
            binding.root.setOnClickListener(this)
        }

        fun bindProjectItem(projectItem: ProjectItem) {
            this.url = projectItem.url
            binding.host.text = projectItem.host
            projectItem.app.run {
                binding.app.visibility = View.VISIBLE
                binding.app.text = this
            }
            projectItem.realm?.run {
                binding.realm.visibility = View.VISIBLE
                binding.realm.text = this
            }
        }

        override fun onClick(view: View?) {
            goToMainActivity(url)
        }
    }
}

