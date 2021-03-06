/*
 * Copyright (c) 1999-2006 Juancarlo A�ez
 * All rights reserved.
 *
 * $Id: MyersDiff.java,v 1.4 2006/06/08 05:47:32 juanca Exp $
 *
 * For usage rights please see the LICENSE.* files that 
 * come bundled with legal distributions of this resource.
 *  
 * Fore more information please email Juancarlo A�ez at:
 *      juanca@suigeneris.org
 *      juancarlo.anez@gmail.com
 */

package org.suigeneris.jrcs.diff.myers;

import org.suigeneris.jrcs.diff.*;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.diff.delta.Delta;

/**
 * A clean-room implementation of <a
 * href="http://www.cs.arizona.edu/people/gene/"> Eugene Myers</a> differencing
 * algorithm.
 * <p>
 * See the paper at <a
 * href="http://www.eecs.berkeley.edu/~gene//Papers/diff.pdf">
 * http://www.eecs.berkeley.edu/~gene//Papers/diff.pdf</a>
 * 
 * @version $Revision: 1.4 $ $Date: 2006/06/08 05:47:32 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @see Delta
 * @see Revision
 * @see Diff
 */
public class MyersDiff implements DiffAlgorithm
{
    /**
     * Constructs an instance of the Myers differencing algorithm.
     */
    public MyersDiff()
    {
    }

    /**
     * {@inheritDoc}
     */
    public Revision diff(Object[] orig, Object[] rev)
            throws DifferentiationFailedException
    {
        PathNode path = buildPath(orig, rev);
        return buildRevision(path, orig, rev);
    }

    /**
     * Computes the minimum diffpath that expresses de differences between the
     * original and revised sequences, according to Gene Myers differencing
     * algorithm.
     * 
     * @param orig
     *            The original sequence.
     * @param rev
     *            The revised sequence.
     * @return A minimum {@link PathNode Path} accross the differences graph.
     * @throws DifferentiationFailedException
     *             if a diff path could not be found.
     */
    public static PathNode buildPath(Object[] orig, Object[] rev)
            throws DifferentiationFailedException
    {
        if (orig == null)
            throw new IllegalArgumentException("original sequence is null");
        if (rev == null)
            throw new IllegalArgumentException("revised sequence is null");

        // these are local constants
        final int N = orig.length;
        final int M = rev.length;

        final int MAX = N + M + 1;
        final int size = 1 + 2 * MAX;
        final int middle = (size + 1) / 2;
        final PathNode diagonal[] = new PathNode[size];

        diagonal[middle + 1] = new Snake(0, -1, null);
        for (int d = 0; d < MAX; d++)
        {
            for (int k = -d; k <= d; k += 2)
            {
                final int kmiddle = middle + k;
                final int kplus = kmiddle + 1;
                final int kminus = kmiddle - 1;
                PathNode prev = null;

                int i;
                if ((k == -d)
                        || (k != d && diagonal[kminus].i < diagonal[kplus].i))
                {
                    i = diagonal[kplus].i;
                    prev = diagonal[kplus];
                }
                else
                {
                    i = diagonal[kminus].i + 1;
                    prev = diagonal[kminus];
                }

                diagonal[kminus] = null; // no longer used

                int j = i - k;

                PathNode node = new DiffNode(i, j, prev);

                // orig and rev are zero-based
                // but the algorithm is one-based
                // that's why there's no +1 when indexing the sequences
                while (i < N && j < M && orig[i].equals(rev[j]))
                {
                    i++;
                    j++;
                }
                if (i > node.i)
                    node = new Snake(i, j, node);

                if (i >= N && j >= M)
                {
                    return node;
                }
                
                diagonal[kmiddle] = node;
            }
            diagonal[middle + d - 1] = null;
        }
        // According to Myers, this cannot happen
        throw new DifferentiationFailedException("could not find a diff path");
    }

    /**
     * Constructs a {@link Revision} from a difference path.
     * 
     * @param path
     *            The path.
     * @param orig
     *            The original sequence.
     * @param rev
     *            The revised sequence.
     * @return A {@link Revision} script corresponding to the path.
     * @throws DifferentiationFailedException
     *             if a {@link Revision} could not be built from the given path.
     */
    public static Revision buildRevision(PathNode path, Object[] orig,
            Object[] rev)
    {
        if (path == null)
            throw new IllegalArgumentException("path is null");
        if (orig == null)
            throw new IllegalArgumentException("original sequence is null");
        if (rev == null)
            throw new IllegalArgumentException("revised sequence is null");

        Revision revision = new Revision();
        if (path.isSnake())
            path = path.prev;
        while (path != null && path.prev != null && path.prev.j >= 0)
        {
            if (path.isSnake())
                throw new IllegalStateException(
                        "bad diffpath: found snake when looking for diff");
            int i = path.i;
            int j = path.j;

            path = path.prev;
            int ianchor = path.i;
            int janchor = path.j;

            Delta delta = Delta.newDelta(new Chunk(orig, ianchor, i - ianchor),
                    new Chunk(rev, janchor, j - janchor));
            revision.insertDelta(delta);
            if (path.isSnake())
                path = path.prev;
        }
        return revision;
    }

}
