package dev.fritz2.repositories.rest

import dev.fritz2.binding.RootStore
import dev.fritz2.binding.action
import dev.fritz2.binding.each
import dev.fritz2.binding.handledBy
import dev.fritz2.dom.html.render
import dev.fritz2.dom.mount
import dev.fritz2.identification.uniqueId
import dev.fritz2.lenses.buildLens
import dev.fritz2.repositories.Resource
import dev.fritz2.serialization.Serializer
import dev.fritz2.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlin.browser.document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class RestTests {
    data class RestPerson(val name: String, val age: Int, val _id: String = "")

    private val nameLens = buildLens("name", RestPerson::name) { p, v -> p.copy(name = v) }
    private val ageLens = buildLens("age", RestPerson::age) { p, v -> p.copy(age = v) }
    private val idLens = buildLens("_id", RestPerson::_id) { p, v -> p.copy(_id = v) }

    object PersonSerializer : Serializer<RestPerson, String> {
        data class PersonWithoutId(val name: String, val age: Int)

        private fun removeId(person: RestPerson) = PersonWithoutId(person.name, person.age)

        override fun write(item: RestPerson): String = JSON.stringify(removeId(item))
        override fun read(msg: String): RestPerson {
            val obj = JSON.parse<dynamic>(msg)
            return RestPerson(obj.name as String, obj.age as Int, obj._id as String)
        }

        override fun writeList(items: List<RestPerson>): String = JSON.stringify(items.map { removeId(it) })
        override fun readList(msg: String): List<RestPerson> {
            val list = JSON.parse<Array<dynamic>>(msg)
            return list.map { obj -> RestPerson(obj.name as String, obj.age as Int, obj._id as String) }
        }
    }

    @Test
    fun testEntityService() = runTest {
        initDocument()

        val startPerson = RestPerson("Heinz", 18)
        val changedAge = 99

        val personResource = Resource(
            RestPerson::_id,
            PersonSerializer,
            RestPerson("", 0)
        )

        val remote = testServer(rest)

        val entityStore = object : RootStore<RestPerson>(personResource.emptyEntity) {
            override fun errorHandler(exception: Throwable, oldValue: RestPerson): RestPerson {
                fail(exception.message)
            }

            private val rest = restEntity(personResource, "", remote = remote)

            val load = handle { entity, id: String -> rest.load(entity, id) }
            val saveOrUpdate = handleAndOffer<Unit> { entity -> rest.saveOrUpdate(entity) }
            val delete = handleAndOffer<Unit> { entity -> rest.delete(entity) }
        }

        val idId = "id-${uniqueId()}"
        val idSubStore = entityStore.sub(idLens)
        val nameId = "name-${uniqueId()}"
        val nameSubStore = entityStore.sub(nameLens)
        val ageId = "age-${uniqueId()}"
        val ageSubStore = entityStore.sub(ageLens)

        render {
            div {
                div(id = idId) { idSubStore.data.bind() }
                div(id = nameId) { nameSubStore.data.bind() }
                div(id = ageId) { ageSubStore.data.map { it.toString() }.bind() }
            }
        }.mount(targetId)

        action(startPerson) handledBy entityStore.update
        delay(100)

        val nameAfterStart = document.getElementById(nameId)?.textContent
        assertEquals(startPerson.name, nameAfterStart, "no name after start")

        action() handledBy entityStore.saveOrUpdate
        delay(100)

        val idAfterSave = document.getElementById(idId)?.textContent
        assertTrue(idAfterSave?.length ?: 0 > 10, "no id after save")

        action(data = changedAge) handledBy ageSubStore.update
        action() handledBy entityStore.saveOrUpdate
        delay(100)

        val ageAfterUpdate = document.getElementById(ageId)?.textContent
        assertEquals(changedAge.toString(), ageAfterUpdate, "wrong age after update")

        action(data = 0) handledBy ageSubStore.update
        action(idAfterSave.orEmpty()) handledBy entityStore.load
        delay(100)

        val ageAfterLoad = document.getElementById(ageId)?.textContent
        assertEquals(changedAge.toString(), ageAfterLoad, "wrong age after load")

        action() handledBy entityStore.delete
        delay(100)

        val idAfterDelete = document.getElementById(idId)?.textContent
        assertEquals(startPerson._id, idAfterDelete, "wrong id after delete")
    }


    @Test
    fun testQueryService() = runTest {
        initDocument()

        val testList = listOf(
            RestPerson("A", 0),
            RestPerson("B", 1),
            RestPerson("C", 0)
        )

        val personResource = Resource(
            RestPerson::_id,
            PersonSerializer,
            RestPerson("", 0)
        )

        val remote = testServer(rest)

        val queryStore = object : RootStore<List<RestPerson>>(emptyList()) {
            override fun errorHandler(exception: Throwable, oldValue: List<RestPerson>): List<RestPerson> {
                fail(exception.message)
            }

            private val rest = restQuery<RestPerson, String, Unit>(personResource, "", remote = remote)

            val addOrUpdate = handle<RestPerson> { entities, person -> rest.addOrUpdate(entities, person) }
            val query = handle<Unit> { entities, query -> rest.query(entities, query) }
            val delete = handle<String> { entities, id -> rest.delete(entities, id) }
        }

        val listId = "list-${uniqueId()}"
        val firstPersonId = "first-${uniqueId()}"

        render {
            div {
                ul(id = listId) {
                    queryStore.each(RestPerson::_id).render { p ->
                        li { p.data.map { it.name }.bind() }
                    }.bind()
                }
                span(id = firstPersonId) {
                    queryStore.data.map {
                        if (it.isEmpty()) ""
                        else it.first()._id
                    }.bind()
                }
            }
        }.mount(targetId)

        testList.forEach {
            action(it) handledBy queryStore.addOrUpdate
            delay(1)
        }

        delay(200)

        action() handledBy queryStore.query
        delay(200)

        val listAfterQuery = document.getElementById(listId)?.textContent
        assertEquals(testList.joinToString("") { it.name }, listAfterQuery, "wrong list after query")

        val firstId = document.getElementById(firstPersonId)?.textContent
        assertTrue(firstId != null && firstId.length > 10)

        action(firstId) handledBy queryStore.delete
        delay(100)

        val listAfterDelete = document.getElementById(listId)?.textContent
        assertEquals(testList.drop(1).joinToString("") { it.name }, listAfterDelete, "wrong list after delete")

        action(emptyList<RestPerson>()) handledBy queryStore.update
        delay(1)
        action() handledBy queryStore.query
        delay(200)

        val listAfterDeleteAndQuery = document.getElementById(listId)?.textContent
        assertEquals(testList.drop(1).joinToString("") { it.name }, listAfterDeleteAndQuery, "wrong list after query")
    }

    @Test
    fun testQueryServiceUpdate() = runTest {
        initDocument()

        val testList = listOf(
            RestPerson("A", 0),
            RestPerson("B", 1),
            RestPerson("C", 0),
            RestPerson("D", 0)
        )

        val personResource = Resource(
            RestPerson::_id,
            PersonSerializer,
            RestPerson("", 0)
        )

        val remote = testServer(rest)

        val queryStore = object : RootStore<List<RestPerson>>(emptyList()) {
            override fun errorHandler(exception: Throwable, oldValue: List<RestPerson>): List<RestPerson> {
                fail(exception.message)
            }

            private val rest = restQuery<RestPerson, String, Unit>(personResource, "", remote = remote)

            val addOrUpdate = handle<RestPerson> { entities, entity ->
                rest.addOrUpdate(entities, entity)
            }
            val updateMany = handle { entities ->
                rest.updateMany(entities, entities.map { it.copy(name = "${it.name}2") })
            }
            val updateSingle = handle { entities ->
                rest.addOrUpdate(entities, entities[2].copy(name = "C3"))
            }
        }

        val listId = "list-${uniqueId()}"

        render {
            div {
                ul(id = listId) {
                    queryStore.each(RestPerson::_id).render { p ->
                        li { p.data.map { it.name }.bind() }
                    }.bind()
                }
            }
        }.mount(targetId)

        testList.forEach {
            action(it) handledBy queryStore.addOrUpdate
            delay(1)
        }

        delay(300)
        val listAfterAdd = document.getElementById(listId)?.textContent
        assertEquals(testList.joinToString("") { it.name }, listAfterAdd, "wrong list after adding")

        val updatedTestList = testList.map { it.copy(name = "${it.name}2") }
        action() handledBy queryStore.updateMany
        delay(400)

        val listAfterUpdateMany = document.getElementById(listId)?.textContent
        assertEquals(updatedTestList.joinToString("") { it.name }, listAfterUpdateMany, "wrong list after update many")

        action() handledBy queryStore.updateSingle
        delay(200)
        val listAfterUpdate = document.getElementById(listId)?.textContent
        assertEquals(updatedTestList.map { if (it.name == "C2") it.copy(name = "C3") else it }
            .joinToString("") { it.name }, listAfterUpdate, "wrong list after update")
    }
}