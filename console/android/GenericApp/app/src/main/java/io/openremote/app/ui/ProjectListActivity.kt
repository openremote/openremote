package io.openremote.app.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.openremote.app.R
import io.openremote.app.databinding.ActivityProjectListBinding
import io.openremote.app.model.ProjectItem
import io.openremote.app.util.Constants
import io.openremote.orlib.ORConstants
import io.openremote.orlib.ui.OrMainActivity


class ProjectListActivity : AppCompatActivity() {
    lateinit var binding: ActivityProjectListBinding
    lateinit var projectListAdapter: ProjectListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProjectListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val toolbar: Toolbar =  findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val projectItems = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.PROJECT_LIST, null)?.let {
            jacksonObjectMapper().readValue<List<ProjectItem>>(it).toMutableList()
        } ?: mutableListOf()

        projectListAdapter = ProjectListAdapter(projectItems.toMutableList().asReversed(), this::goToMainActivity)
        binding.projectList.adapter = projectListAdapter
        binding.projectList.layoutManager = LinearLayoutManager(this);

        val background = ColorDrawable(Color.parseColor("#bf1515"))
        val xMark = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel)?.apply {
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        val xMarkMargin = 24

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val projectItems = projectListAdapter.items.toMutableList().asReversed()
                projectItems.removeAt(viewHolder.adapterPosition)
                PreferenceManager.getDefaultSharedPreferences(this@ProjectListActivity)
                    .edit()
                    .putString(Constants.PROJECT_LIST, jacksonObjectMapper().writeValueAsString(projectItems))
                    .apply()
                projectListAdapter.remove(viewHolder.adapterPosition)
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (viewHolder.adapterPosition < 0) return

                val itemView = viewHolder.itemView // the view being swiped

                // draw the red background, based on the offset of the swipe (dX)
                background.apply {
                    setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    draw(c)
                }

                // draw the symbol
                xMark?.apply {
                    val xt = itemView.top + (itemView.bottom - itemView.top - xMark.intrinsicHeight) / 2
                    setBounds(
                        itemView.right - xMarkMargin - xMark.intrinsicWidth,
                        xt,
                        itemView.right - xMarkMargin,
                        xt + xMark.intrinsicHeight
                    )
                    draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val touchHelper = ItemTouchHelper(swipeHandler)
        touchHelper.attachToRecyclerView(binding.projectList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.projects_menu, menu)
        val menuItem = menu?.findItem(R.id.add);
        menuItem?.actionView?.setOnClickListener {
            this.onOptionsItemSelected(menuItem)
        }
        menuItem?.actionView?.setPadding(10)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.add -> {
                val intent = Intent(this, ProjectWizardActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToMainActivity(url: String) {
        val intent = Intent(this, OrMainActivity::class.java)
        intent.putExtra(ORConstants.BASE_URL_KEY, url)
        runOnUiThread {
            startActivity(intent)
            finish()
        }
    }
}
