// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.observable.operations

import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.CopyOnWriteArrayList

class CompoundParallelOperationTrace<Id>(private val debugName: String? = null) {

  private val traces = LinkedHashMap<Id, Int>()
  private var waitForFirstTaskInOperation = false
  private var isOperationCompleted = true

  private val beforeOperationListeners = CopyOnWriteArrayList<() -> Unit>()
  private val afterOperationListeners = CopyOnWriteArrayList<() -> Unit>()

  fun startOperation() {
    synchronized(this) {
      waitForFirstTaskInOperation = true
      if (!isOperationCompleted) return
      isOperationCompleted = false
    }
    debug("Operation is started")
    beforeOperationListeners.forEach { it() }
  }

  fun isOperationCompleted(): Boolean {
    synchronized(this) {
      return isOperationCompleted
    }
  }

  fun startTask(taskId: Id) {
    synchronized(this) {
      waitForFirstTaskInOperation = false
      addTask(taskId)
    }
  }

  fun finishTask(taskId: Id) {
    synchronized(this) {
      if (!removeTask(taskId)) return
      if (traces.isNotEmpty()) return
      if (waitForFirstTaskInOperation) return
      isOperationCompleted = true
    }
    debug("Operation is finished")
    afterOperationListeners.forEach { it() }
  }

  private fun addTask(taskId: Id) {
    val taskCounter = traces.getOrPut(taskId) { 0 }
    traces[taskId] = taskCounter + 1
    debug("Task is started with id `$taskId`")
  }

  private fun removeTask(taskId: Id): Boolean {
    debug("Task is finished with id `$taskId`")
    val taskCounter = traces[taskId] ?: return false
    when (taskCounter) {
      1 -> traces.remove(taskId)
      else -> traces[taskId] = taskCounter - 1
    }
    return taskCounter == 1
  }

  fun beforeOperation(listener: Listener) {
    beforeOperation(listener::listen)
  }

  fun beforeOperation(listener: () -> Unit) {
    beforeOperationListeners.add(listener)
  }

  fun afterOperation(listener: Listener) {
    afterOperation(listener::listen)
  }

  fun afterOperation(listener: () -> Unit) {
    afterOperationListeners.add(listener)
  }

  private fun debug(message: String) {
    if (LOG.isDebugEnabled) {
      val debugPrefix = if (debugName == null) "" else "$debugName: "
      LOG.debug("$debugPrefix$message")
    }
  }

  interface Listener {
    fun listen()
  }

  companion object {
    private val LOG = Logger.getInstance(CompoundParallelOperationTrace::class.java)
  }
}