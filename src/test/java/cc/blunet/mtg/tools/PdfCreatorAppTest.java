package cc.blunet.mtg.tools;

import static cc.blunet.mtg.db.Repository.defaultFilters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import cc.blunet.mtg.core.AdvDeckFactory;
import cc.blunet.mtg.core.DeckFactory;
import cc.blunet.mtg.core.PrintedDeck;
import cc.blunet.mtg.db.Repository;

public class PdfCreatorAppTest {
  private static final AdvDeckFactory deckFactory =
      new AdvDeckFactory(new DeckFactory(new Repository(defaultFilters())));

  public static void main(String[] args) throws URISyntaxException, IOException {
    // given
    Path root = Paths.get(PdfCreatorAppTest.class.getResource(".").toURI());
    Set<PrintedDeck> decks = deckFactory.read(root.resolve("decklist.txt"));
    Set<PrintedDeck> collection = Collections.emptySet();
    Path imagesPath = root;
    Path resultPath = root;

    // when
    new PdfCreatorApp().createPdf(decks, collection, imagesPath, resultPath);

    // then
  }
}
