/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package network;

/**
 * Base class for network unit tests.
 * For headless libGDX tests, see HeadlessNetworkTest.
 */
public abstract class NetworkTestBase {

    /**
     * Helper method to check if a string is not null or empty.
     */
    protected boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
