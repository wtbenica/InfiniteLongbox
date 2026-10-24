/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.benica.infinite_longbox.nav

/** What a change of the filter does to the stack. */
enum class ListMove {
    /** Nothing is on screen yet: the first list. */
    START,

    /** The list showing takes the change. */
    APPLY,

    /** Only the view changed (series, characters, creators): the list takes the place of the one showing. */
    REPLACE,

    /** Another list on top; Back returns to what was there. */
    PUSH,
}

/**
 * Decides what the filter now asking for a [wanted] list does, with [top] on screen.
 * [sameItems] is asked only when it matters: whether the filter still picks the same items as the top list's.
 */
fun listMove(top: Route?, wanted: ListKind, sameItems: () -> Boolean): ListMove = when {
    top == null -> ListMove.START
    top is Route.List && top.kind == wanted -> ListMove.APPLY
    top is Route.List && sameItems() -> ListMove.REPLACE
    else -> ListMove.PUSH
}
