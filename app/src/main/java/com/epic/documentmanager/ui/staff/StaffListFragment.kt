package com.epic.documentmanager.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.epic.documentmanager.R
import com.epic.documentmanager.viewmodels.StaffViewModel

class StaffListFragment : Fragment() {

    private val vm: StaffViewModel by viewModels()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: StaffAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_staff_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe    = view.findViewById(R.id.swipe)
        recycler = view.findViewById(R.id.recycler)
        etSearch = view.findViewById(R.id.etSearch)
        tvEmpty  = view.findViewById(R.id.tvEmpty)

        adapter = StaffAdapter(
            onEdit = { user ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, StaffFormFragment.newInstance(user.uid))
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { user -> vm.deleteUser(user.uid) {
                Toast.makeText(requireContext(), "Akun dihapus.", Toast.LENGTH_SHORT).show()
            } }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        recycler.adapter = adapter
        val fab: View = view.findViewById(R.id.fabAdd)
        fab.setOnClickListener {
            startActivity(
                android.content.Intent(
                    requireContext(),
                    com.epic.documentmanager.ui.staff.CreateStaffActivity::class.java
                )
            )
        }

        swipe.setOnRefreshListener { vm.refresh() }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                vm.search(etSearch.text?.toString().orEmpty())
                true
            } else false
        }

        vm.users.observe(viewLifecycleOwner) {
            adapter.submit(it)
            tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.loading.observe(viewLifecycleOwner) { swipe.isRefreshing = it }
        vm.error.observe(viewLifecycleOwner) { it?.let { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        } }

        // muat data
        vm.startListening()
    }
}
