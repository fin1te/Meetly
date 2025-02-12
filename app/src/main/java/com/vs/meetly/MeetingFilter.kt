@file:Suppress("DEPRECATION")

package com.vs.meetly

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vs.meetly.adapters.IMeetingRVAdapter
import com.vs.meetly.adapters.MeetingAdapter
import com.vs.meetly.modals.Meeting
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_meeting_filter.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MeetingFilter : AppCompatActivity(), IMeetingRVAdapter {

    lateinit var adapter: MeetingAdapter

    private var meetingList = mutableListOf<Meeting>()

    private var tempMeetingList = mutableListOf<Meeting>()

    lateinit var firestore: FirebaseFirestore

    lateinit var DATE : String

    lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_filter)

        auth = FirebaseAuth.getInstance()

         DATE = intent.getStringExtra("DATE").toString()
        setUpFireStore(DATE)
        setUpRecyclerView()

        topAppBar.setNavigationOnClickListener {
            finish()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK
            && requestCode == MainActivity.MEETING_VIEW_DETAIL_CODE
        ) {
            Toast.makeText(this, "Meeting View Detail!", Toast.LENGTH_SHORT).show()
            setUpFireStore(DATE)
            setUpRecyclerView()
            adapter.notifyDataSetChanged()
        }
        else {
            Log.e("Cancelled", "Cancelled")
        }
    }


    private fun setUpFireStore(DATE: String) {
        firestore = FirebaseFirestore.getInstance()
        val collectionReference = firestore.collection("meetings").whereEqualTo("date", DATE).whereArrayContains("userId", auth.currentUser!!.uid)
        collectionReference.addSnapshotListener { value, error ->
            if (value == null || error != null) {
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            meetingList.clear()
            meetingList.addAll(value.toObjects(Meeting::class.java))
            if(meetingList.isEmpty()){
                mainNoFilterData.visibility = View.VISIBLE
            }
            else{
                mainNoFilterData.visibility = View.GONE
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setUpRecyclerView() {
        adapter = MeetingAdapter(this, meetingList, this)
        meetingFilterRecyclerView.layoutManager = LinearLayoutManager(this)
        meetingFilterRecyclerView.adapter = adapter
    }

    override fun onItemClicked(meeting: Meeting) {
//        Toast.makeText(this, meeting.content, Toast.LENGTH_SHORT).show()
        val meetingColRef = firestore.collection("meetings")
        CoroutineScope(Dispatchers.IO).launch {
            val meetingQuery = meetingColRef
                .whereEqualTo("content", meeting.content)
                .whereEqualTo("title", meeting.title)
                .whereEqualTo("meeting_link", meeting.meeting_link)
                .whereEqualTo("date", meeting.date)
                .whereEqualTo("time", meeting.time)
                .whereEqualTo("userId", meeting.userId)
                .whereEqualTo("color", meeting.color)
                .get()
                .await()
            if(meetingQuery.documents.isNotEmpty()) {
                for(document in meetingQuery) {
                    try {
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(
                                this@MeetingFilter,
                                R.style.Base_ThemeOverlay_MaterialComponents_MaterialAlertDialog
                            )
                                .setMessage(resources.getString(R.string.confirm_logout))
                                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                                    // Respond to negative button press
                                }
                                .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->
                                    GlobalScope.launch {
                                        meetingColRef.document(document.id).delete().await()
                                        withContext(Dispatchers.Main){
                                            adapter.notifyDataSetChanged()
                                            setResult(Activity.RESULT_OK)
                                        }
                                    }
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MeetingFilter, e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MeetingFilter, "No meetings matched the query.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun getIntoActivity(meeting: Meeting) {
        CoroutineScope(Dispatchers.IO).launch {
            val meetingColRef = firestore.collection("meetings")
            val meetingQuery = meetingColRef
                .whereEqualTo("content", meeting.content)
                .whereEqualTo("title", meeting.title)
                .whereEqualTo("meeting_link", meeting.meeting_link)
                .whereEqualTo("date", meeting.date)
                .whereEqualTo("time", meeting.time)
                .whereEqualTo("userId", meeting.userId)
                .whereEqualTo("color", meeting.color)
                .get()
                .await()
            if (meetingQuery.documents.isNotEmpty()) {
                for (document in meetingQuery) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MeetingFilter, document.id, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@MeetingFilter, MeetingViewDetail::class.java)
                        intent.putExtra("meeting_document_id", document.id)
                        startActivityForResult(intent, MainActivity.MEETING_VIEW_DETAIL_CODE)
                    }
                }
            }
        }
    }
}