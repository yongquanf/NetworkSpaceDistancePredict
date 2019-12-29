/**
 * News Project
 *
 * File:         Randomness.java
 * RCS:          $Id: Randomness.java,v 1.2 2008/05/14 14:24:13 drc915 Exp $
 * Description:  Randomness class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 8, 2006 at 9:18:12 AM
 * Language:     Java
 * Package:      edu.northwestern.news.util
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.NUDT.PDL.util;

import java.util.Random;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Randomness class ...
 */
public class Randomness {
    private static Randomness me;
    private Random r;

    /**
     *
     */
    private Randomness() {
        super();
        r = new Random();
    }

    public static Random getRandom() {
        if (me == null) {
            me = new Randomness();
        }

        return me.r;
    }
}
