package com.wtb.comiccollector.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.FullCharacter
import com.wtb.comiccollector.fragments_view_models.CharacterListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "CharacterListFragment"

@ExperimentalCoroutinesApi
class CharacterListFragment : ListFragment<FullCharacter, CharacterListFragment.CharacterHolder>() {

    override val viewModel: CharacterListViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)
    override fun getAdapter(): CharacterAdapter = CharacterAdapter()


    inner class CharacterAdapter :
        PagingDataAdapter<FullCharacter, CharacterHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterHolder =
            CharacterHolder(parent)

        override fun onBindViewHolder(holder: CharacterHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }
    }

    inner class CharacterHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple, parent, false)
    ), View.OnClickListener {

        private lateinit var item: FullCharacter
        private val nameTextView: TextView =
            itemView.findViewById(R.id.list_item_character_name_text)
        private val alterEgoTextView: TextView = itemView.findViewById(R.id.list_item_alter_ego)
        private val publisherTextView: TextView =
            itemView.findViewById(R.id.list_item_char_publisher)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: FullCharacter) {
            this.item = item
            nameTextView.text = this.item.character.name
            alterEgoTextView.text = this.item.character.alterEgo
            publisherTextView.text = this.item.publisher.publisher
        }

        override fun onClick(v: View?) {
            (callback as CharacterListCallback?)?.onCharacterSelected(item.character)
        }
    }

    interface CharacterListCallback : ListFragmentCallback {
        fun onCharacterSelected(character: Character)
    }

    companion object {
        @JvmStatic
        fun newInstance() = CharacterListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullCharacter>() {
            override fun areItemsTheSame(oldItem: FullCharacter, newItem: FullCharacter): Boolean =
                oldItem.character.characterId == newItem.character.characterId


            override fun areContentsTheSame(
                oldItem: FullCharacter,
                newItem: FullCharacter,
            ): Boolean =
                oldItem == newItem
        }
    }

}