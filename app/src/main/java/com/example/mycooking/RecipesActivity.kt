package com.example.mycooking

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button

class RecipesActivity : AppCompatActivity() {
    private val categoryMap = LinkedHashMap<String, MutableList<Pair<String, String>>>()
    private var header: String = ""
    private var closer: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipes)

        val dishesText = intent.getStringExtra("dishes") ?: "No dishes available"
        displayRecipes(dishesText)
        setupBackButton()
    }

    private fun displayRecipes(dishesText: String) {
        val lines = dishesText.split("\n")
        var currentCategory = ""
        var isHeader = true
        var isClosingRemark = false

        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                isHeader && trimmedLine.endsWith(":") -> {
                    // Header
                    header = trimmedLine
                    isHeader = false
                }
                trimmedLine.endsWith(":") && !isClosingRemark -> {
                    // Categories
                    currentCategory = cleanText(trimmedLine)
                    categoryMap[currentCategory] = mutableListOf()
                }
                trimmedLine.contains(":") && !isClosingRemark -> {
                    // Dish with a recipe
                    val (dishName, recipe) = trimmedLine.split(":", limit = 2)
                    val cleanedDishName = cleanText(dishName)
                    val cleanedRecipe = cleanText(recipe)
                    categoryMap[currentCategory]?.add(Pair(cleanedDishName, cleanedRecipe))
                }
                trimmedLine.isNotEmpty() && !isHeader && categoryMap.isNotEmpty() -> {
                    // Closing remark
                    isClosingRemark = true
                    closer = trimmedLine
                }
            }
        }

        // Filter out empty categories
        val nonEmptyCategoryMap = categoryMap.filterValues { it.isNotEmpty() }

        setupHeader()
        setupExpandableListView(nonEmptyCategoryMap)
        setupCloser()
    }

    private fun cleanText(text: String): String {
        return text.replace(":", "").trim()
    }

    private fun setupExpandableListView(filteredCategoryMap: Map<String, MutableList<Pair<String, String>>>) {
        val expandableListView = findViewById<ExpandableListView>(R.id.expandableListView)
        val adapter = CustomExpandableListAdapter(this, ArrayList(filteredCategoryMap.keys), filteredCategoryMap)
        expandableListView.setAdapter(adapter)
    }

    private fun setupHeader() {
        val headerTextView = findViewById<TextView>(R.id.headerTextView)
        headerTextView.text = header
    }

    private fun setupCloser() {
        val closerTextView = findViewById<TextView>(R.id.closerTextView)
        closerTextView.text = closer
    }

    private fun setupBackButton() {
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.action = "retakePhoto"
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}
// Adapter class of the expandable list view
class CustomExpandableListAdapter(
    private val context: Context,
    private val categoryList: List<String>,
    private val categoryMap: Map<String, List<Pair<String, String>>>
) : BaseExpandableListAdapter() {

    override fun getChild(groupPosition: Int, childPosition: Int): Pair<String, String> {
        return categoryMap[categoryList[groupPosition]]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        var itemView = convertView
        val childViewHolder: ChildViewHolder

        if (itemView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            itemView = inflater.inflate(R.layout.list_item_dish, parent, false)

            childViewHolder = ChildViewHolder()
            childViewHolder.tvDishName = itemView.findViewById(R.id.tvDishName)
            childViewHolder.tvRecipe = itemView.findViewById(R.id.tvRecipe)

            itemView.tag = childViewHolder
        } else {
            childViewHolder = itemView.tag as ChildViewHolder
        }

        val (dishName, recipe) = getChild(groupPosition, childPosition)
        childViewHolder.tvDishName.text = dishName
        childViewHolder.tvRecipe.text = recipe

        return itemView!!
    }

    override fun getChildrenCount(groupPosition: Int): Int = categoryMap[categoryList[groupPosition]]?.size ?: 0

    override fun getGroup(groupPosition: Int): String = categoryList[groupPosition]

    override fun getGroupCount(): Int = categoryList.size

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var itemView = convertView
        val groupViewHolder: GroupViewHolder
        if (itemView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            itemView = inflater.inflate(R.layout.list_item_category, parent, false)
            groupViewHolder = GroupViewHolder()
            groupViewHolder.tvCategory = itemView.findViewById(R.id.tvCategory)
            groupViewHolder.tvArrow = itemView.findViewById(R.id.tvArrow)
            itemView.tag = groupViewHolder
        } else {
            groupViewHolder = itemView.tag as GroupViewHolder
        }

        groupViewHolder.tvCategory.text = getGroup(groupPosition)

        groupViewHolder.tvArrow.visibility = View.VISIBLE
        groupViewHolder.tvArrow.text = if (isExpanded) "\u2191" else "\u2193"

        return itemView!!
    }

    override fun hasStableIds(): Boolean = true

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    private class GroupViewHolder {
        lateinit var tvCategory: TextView
        lateinit var tvArrow: TextView
    }

    private class ChildViewHolder {
        lateinit var tvDishName: TextView
        lateinit var tvRecipe: TextView
    }
}
