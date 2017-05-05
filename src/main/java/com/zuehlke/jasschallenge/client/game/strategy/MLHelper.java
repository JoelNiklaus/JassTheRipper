package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.game.cards.Card;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MLHelper {

	public static Instances loadArff(String filePath) {
		Instances data = null;
		try {
			data = new ConverterUtils.DataSource(filePath).getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		data.setClassIndex(0);
		return data;
	}

	public static String predictTrumpf(Classifier classifier, Instance cards) {
		double predictedClass = 0;
		try {
			predictedClass = classifier.classifyInstance(cards);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cards.classAttribute().value((int) predictedClass);
	}

	public static Instance buildInstance(Instances train, Set<Card> availableCards) {
		double[] instanceValues = new double[train.numAttributes()];
		int index = 0;

		// trump
		instanceValues[index] = 0;
		index++;

		// cards
		for (Card card : availableCards) {
			instanceValues[index] = train.attribute("card" + index).indexOfValue(card.toString());
			index++;
		}

		return new DenseInstance(1.0, instanceValues);
	}
}
