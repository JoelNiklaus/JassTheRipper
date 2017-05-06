package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.strategy.exceptions.InvalidTrumpfException;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.MLHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.InstanceComparator;
import weka.core.Instances;

import java.util.EnumSet;
import java.util.Set;

import static com.zuehlke.jasschallenge.game.cards.Card.*;
import static org.junit.Assert.*;

/**
 * Created by joelniklaus on 06.05.17.
 */
public class MLHelperTest {
	@Test
	public void loadArff() throws Exception {
		Instances train = MLHelper.loadArff("ml/trumpTrain.arff");
		assertNotEquals(null, train);
		assertEquals("trumpf", train.relationName());
		assertEquals("trumpf", train.classAttribute().name());
		assertEquals("card1", train.attribute(1).name());
	}

	@Test
	public void buildInstance() throws Exception {
		Instances train = MLHelper.loadArff("ml/trumpTrain.arff");
		Set<Card> cards = EnumSet.of(HEART_ACE, HEART_SIX, HEART_TEN, HEART_JACK, CLUB_JACK, CLUB_EIGHT, DIAMOND_KING, DIAMOND_EIGHT, SPADE_EIGHT);
		Instances actual = MLHelper.buildSingleInstanceInstances(train, cards);

		double[] instanceValues = new double[train.numAttributes()];
		int index = 0;

		//trumpf
		instanceValues[index] = 0;
		index++;

		// cards
		instanceValues[index] = 0;
		index++;
		instanceValues[index] = 4;
		index++;
		instanceValues[index] = 5;
		index++;
		instanceValues[index] = 8;
		index++;
		instanceValues[index] = 11;
		index++;
		instanceValues[index] = 16;
		index++;
		instanceValues[index] = 20;
		index++;
		instanceValues[index] = 23;
		index++;
		instanceValues[index] = 29;
		index++;

		Instances expected = new Instances(train);
		expected.delete();
		expected.setClassIndex(0);
		expected.add(new DenseInstance(1.0, instanceValues));

		assertTrue(new InstanceComparator().compare(expected.firstInstance(), actual.firstInstance()) == 0);
	}

	@Test
	public void predictTrumpf() throws Exception {
		Classifier classifier = new NaiveBayes();
		Instances train = MLHelper.loadArff("ml/trumpTrain.arff");
		classifier.buildClassifier(train);
		Set<Card> cards = EnumSet.of(HEART_ACE, HEART_SIX, HEART_TEN, HEART_JACK, CLUB_JACK, CLUB_EIGHT, DIAMOND_KING, DIAMOND_EIGHT, SPADE_EIGHT);
		Instances cardInstances = MLHelper.buildSingleInstanceInstances(train, cards);
		Mode actual = null;
		try {
			actual = MLHelper.predictTrumpf(classifier, cardInstances);
		} catch (InvalidTrumpfException e) {
			e.printStackTrace();
		}
		String expected = "OBEABE";
		assertEquals(expected, actual.toString());
	}


}