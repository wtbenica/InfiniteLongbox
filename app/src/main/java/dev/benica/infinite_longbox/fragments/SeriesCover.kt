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

package dev.benica.infinite_longbox.fragments

/**
 * Whether a series card should go and get a cover from another issue of its series: it has none
 * to show, and its first issue has either not been found or has been looked at and has no cover.
 * While the first issue's cover is still being fetched the answer is no: the card refreshes when
 * it arrives.
 */
internal fun shouldFetchSeriesCover(
    hasCover: Boolean,
    firstIssueKnown: Boolean,
    firstIssueCoverChecked: Boolean,
): Boolean = !hasCover && (!firstIssueKnown || firstIssueCoverChecked)
