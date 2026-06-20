package moe.rukamori.composerapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.rukamori.composerapp.data.db.ProjectDao
import moe.rukamori.composerapp.data.db.ProjectEntity
import moe.rukamori.composerapp.domain.model.ComposerProject
import moe.rukamori.composerapp.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProjectRepository
    @Inject
    constructor(
        private val dao: ProjectDao,
        private val codec: ProjectJsonCodec,
    ) : ProjectRepository {
        override fun observeProjects(): Flow<List<ComposerProject>> =
            dao.observeProjects().map { entities ->
                entities.map { it.toProject() }
            }

        override suspend fun getProject(id: String): ComposerProject? = dao.getProject(id)?.toProject()

        override suspend fun saveProject(project: ComposerProject) {
            dao.upsert(project.toEntity())
        }

        override suspend fun deleteProject(id: String) {
            dao.delete(id)
        }

        private fun ProjectEntity.toProject(): ComposerProject = codec.decode(payloadJson)

        private fun ComposerProject.toEntity(): ProjectEntity =
            ProjectEntity(
                id = metadata.id,
                title = metadata.title,
                artist = metadata.artist,
                updatedAtMs = metadata.updatedAtMs,
                payloadJson = codec.encode(this),
            )
    }
