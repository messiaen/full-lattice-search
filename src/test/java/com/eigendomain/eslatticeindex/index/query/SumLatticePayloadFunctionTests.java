/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eigendomain.eslatticeindex.index.query;

import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

public class SumLatticePayloadFunctionTests extends ESTestCase {

    public void testSpanScore() {
        LatticePayloadScoreFunction function = new SumLatticePayloadFunction(1.0f);
        int start = 1;
        int end = 3;
        float currentScore = 0;

        float expected = 1500.0f;
        float actual = function.spanScore(0, "", start, end, 0, 0, 0, -3.506f);
        Assert.assertEquals(expected, actual, 1);
    }

    public void testSpanScoreLenNorm() {
        LatticePayloadScoreFunction function = new SumLatticePayloadFunction(0.5f);
        int start = 1;
        int end = 3;
        float currentScore = 0;

        float expected = 3000.0f;
        float actual = function.spanScore(0, "", start, end, 0, 0, 0, -3.506f);
        Assert.assertEquals(expected, actual, 2);
    }

    public void testCurrentLeafScore() {
        LatticePayloadScoreFunction function = new SumLatticePayloadFunction(1.0f);
        int start = 1;
        int end = 2;
        float currentScore = 0;
        float expected = -0.510825f;
        float actual = function.currentLeafScore(0, "", start, end, 0, currentScore, 0.6f);
        Assert.assertEquals(expected, actual, 0.000001f);

        currentScore = expected;
        expected = expected + -3.506558f;
        actual = function.currentLeafScore(0, "", start, end, 1, currentScore, 0.03f);
        Assert.assertEquals(expected, actual, 0.000001f);

        expected = SumLatticePayloadFunction.MIN_LOG_SCORE;
        actual = function.currentLeafScore(0, "", start, end, 3, -12.5f, 0.03f);
        Assert.assertEquals(expected, actual, 0.000001f);
    }

    public void testDocScore() {
        LatticePayloadScoreFunction function = new SumLatticePayloadFunction(1.0f);

        float expected = SumLatticePayloadFunction.MIN_SCORE;
        float actual = function.docScore(0, "", 0, 0.9987f);
        Assert.assertEquals(expected, actual, 0.000001);

        expected = 0.12345f;
        actual = function.docScore(0, "", 1, 0.12345f);
        Assert.assertEquals(expected, actual, 0.000001);
    }
}