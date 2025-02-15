package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask
import java.util.concurrent.TimeUnit

class PlaysViewModel(application: Application) : AndroidViewModel(application) {
    private val syncTimestamp = MutableLiveData<Long>()

    data class PlayInfo(
            val mode: Mode,
            val name: String = "",
            val id: Int = BggContract.INVALID_ID,
            val filter: FilterType = FilterType.ALL,
            val sort: SortType = SortType.DATE
    )

    enum class Mode {
        ALL, GAME, BUDDY, PLAYER, LOCATION
    }

    enum class FilterType {
        ALL, DIRTY, PENDING
    }

    enum class SortType {
        DATE, LOCATION, GAME, LENGTH
    }

    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())

    private val playInfo = MutableLiveData<PlayInfo>()

    private val locationRenameCount = MutableLiveData<PlayRepository.RenameLocationResults>()
    val updateMessage: LiveData<Event<String>> = Transformations.map(locationRenameCount) { result ->
        result?.let {
            setLocation(it.newLocationName)
            SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
            Event(getApplication<BggApplication>().resources.getQuantityString(R.plurals.msg_play_location_change, it.count, it.count, it.oldLocationName, it.newLocationName))
        }
    }

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<Event<String>> = Transformations.map(_errorMessage) { Event(it) }

    private val _syncingStatus = MutableLiveData<Boolean>()
    val syncingStatus: LiveData<Boolean>
        get() = _syncingStatus

    private var syncPlaysByDateTask: SyncPlaysByDateTask? = null
    private var syncPlaysByGameTask: SyncPlaysByGameTask? = null

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = Transformations.switchMap(playInfo) {
        when (it.mode) {
            Mode.ALL -> {
                val sortType = when (it.sort) {
                    SortType.DATE -> PlayDao.PlaysSortBy.DATE
                    SortType.LOCATION -> PlayDao.PlaysSortBy.LOCATION
                    SortType.GAME -> PlayDao.PlaysSortBy.GAME
                    SortType.LENGTH -> PlayDao.PlaysSortBy.LENGTH
                }
                when (it.filter) {
                    FilterType.ALL -> playRepository.getPlays(sortType)
                    FilterType.DIRTY -> playRepository.getDraftPlays()
                    FilterType.PENDING -> playRepository.getPendingPlays()
                }
            }
            Mode.GAME -> gameRepository.getPlays(it.id)
            Mode.LOCATION -> playRepository.loadPlaysByLocation(it.name)
            Mode.BUDDY -> playRepository.loadPlaysByUsername(it.name)
            Mode.PLAYER -> playRepository.loadPlaysByPlayerName(it.name)
        }
    }

    val filterType: LiveData<FilterType> = Transformations.map(playInfo) {
        it.filter
    }

    val sortType: LiveData<SortType> = Transformations.map(playInfo) {
        it.sort
    }

    val location: LiveData<String> = Transformations.map(playInfo) {
        if (it.mode == Mode.LOCATION) it.name else ""
    }

    fun setGame(gameId: Int) {
        playInfo.value = PlayInfo(Mode.GAME, id = gameId)
    }

    fun setLocation(locationName: String) {
        playInfo.value = PlayInfo(Mode.LOCATION, locationName)
    }

    fun setUsername(username: String) {
        playInfo.value = PlayInfo(Mode.BUDDY, username)
    }

    fun setPlayerName(playerName: String) {
        playInfo.value = PlayInfo(Mode.PLAYER, playerName)
    }

    fun setFilter(type: FilterType) {
        if (playInfo.value?.filter != type) {
            playInfo.value = PlayInfo(Mode.ALL, filter = type, sort = playInfo.value?.sort ?: SortType.DATE)
        }
    }

    fun setSort(type: SortType) {
        if (playInfo.value?.sort != type) {
            playInfo.value = PlayInfo(Mode.ALL, filter = playInfo.value?.filter ?: FilterType.ALL, sort = type)
        }
    }

    fun refresh(): Boolean {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
        return if ((syncTimestamp.value ?: 0).isOlderThan(1, TimeUnit.MINUTES)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            playInfo.value?.let {
                if (it.mode == Mode.GAME) {
                    syncPlaysByGameTask = SyncPlaysByGameTask(getApplication(), it.id, _errorMessage, _syncingStatus)
                    syncPlaysByGameTask?.executeAsyncTask()
                } else {
                    SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS)
                }
            }
            true
        } else false
    }

    fun renameLocation(oldLocationName: String, newLocationName: String) {
        playRepository.renameLocation(oldLocationName, newLocationName, locationRenameCount)
    }

    fun syncPlaysByDate(timeInMillis: Long) {
        syncPlaysByDateTask = SyncPlaysByDateTask(getApplication(), timeInMillis, _errorMessage, _syncingStatus)
        syncPlaysByDateTask?.executeAsyncTask()
    }

    override fun onCleared() {
        super.onCleared()
        syncPlaysByDateTask?.cancel(true)
        syncPlaysByGameTask?.cancel(true)
    }
}
