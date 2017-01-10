# nlpsquelch
This repo consists of several functionalities illustrating the use of Stanford CoreNLP to add natural language processing capability:
--- ExtractGenerator:  takes an argument which is content in a .txt file. Parses out sentences, assigns them a score, picks the top few and classifies them as "indicative of the overall document" or not. Generates the Document Extract by concactenating this.
--- QueryAnalyzer: takes an argument which is multi-line inputs in a .txt file. For each input text: Parses out sentences, Does Sentiment Analysis and extracts Tokens and highlights "important" tokens along with NER and POS tags.

Sample Text files are committed as examples as well.
