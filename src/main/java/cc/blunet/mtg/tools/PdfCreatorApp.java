package cc.blunet.mtg.tools;

import static cc.blunet.mtg.db.Repository.defaultFilters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import cc.blunet.common.Unchecked;
import cc.blunet.mtg.core.AdvDeckFactory;
import cc.blunet.mtg.core.Card.DoubleFacedCard;
import cc.blunet.mtg.core.DeckFactory;
import cc.blunet.mtg.core.PrintedDeck;
import cc.blunet.mtg.core.PrintedDeck.PrintedCard;
import cc.blunet.mtg.db.Repository;

/**
 * Creates a Pdf with Magic the Gathering Card-Images from Deck files.
 *
 * @author claude.nobs@blunet.cc
 */
public class PdfCreatorApp {

  private static final Path root = Paths.get("/", "Users", "bernstein", "XLHQ-Sets-Torrent");

  // TODO support commandline args?
  // create pdf for tokens/emblems: create a decklist!
  public static void main(String[] args) throws IOException, URISyntaxException {
    // where to find a list of existing cards
    Path collectionPath = root.resolve("_collection");
    // where to search for deck lists
    Path deckPath = root.resolve("_decks");
    // Path deckPath = Paths2.of(PdfCreatorApp.class, "/decks/");
    // where to read the images
    Path imagesPath = root.resolve("_all");
    // where to write the resulting pdf
    Path resultPath = root.resolve("_pdfs/" + "output" + ".pdf");

    // run
    AdvDeckFactory deckFactory = new AdvDeckFactory(new DeckFactory(new Repository(defaultFilters())));

    Predicate<String> collectionFileFilter = n -> true /* your filter */;
    Predicate<String> deckFileFilter = n -> true /* your filter */;

    Set<PrintedDeck> collection = deckFactory.readAll(collectionPath, collectionFileFilter);
    Set<PrintedDeck> decks = deckFactory.readAll(deckPath, deckFileFilter);
    new PdfCreatorApp().createPdf(decks, collection, imagesPath, resultPath);
  }

  private static final float POINTS_PER_MM = 2.834646F;

  public void createPdf(Set<PrintedDeck> decks, Set<PrintedDeck> collection, Path imagesPath, Path resultPath) {
    List<Float> x = ImmutableList.of(11.0f, 74.0f, 137.0f);
    List<Float> y = ImmutableList.of(16.0f, 104.0f, 192.0f).reverse();

    Map<Path, PDImageXObject> images = new HashMap<>();

    try (final PDDocument document = new PDDocument()) {
      for (Multimap<PrintedDeck, PrintedCard> part : paged(decks, collection)) {

        final PDPage page = new PDPage(PDRectangle.A4);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {

          int i = 0;
          for (PrintedCard card : part.values()) {
            // add image to document (NOTE: images of same path are only added once)
            Path path = imagesPath.resolve(imageName(card));
            if (!images.containsKey(path)) {
              images.put(path, PDImageXObject.createFromFile(path.toString(), document));
            }
            PDImageXObject pdImage = images.get(path);
            // place image in page
            contentStream.drawImage(pdImage, //
                x.get(i % 3) * POINTS_PER_MM, //
                y.get(i / 3) * POINTS_PER_MM, //
                63.0f * POINTS_PER_MM, //
                88.0f * POINTS_PER_MM);
            i++;
          }
          // FIXME blacken card corners/borders
          // write deckname onto page
          String text = part.keySet().stream() //
              .map(deck -> deck.name() + " (" + part.get(deck).size() + ")") //
              .reduce((a, b) -> a + " / " + b) //
              .orElse("");
          drawText(contentStream, 11.0f * POINTS_PER_MM, 285.0f * POINTS_PER_MM, text);
        }
        document.addPage(page);
      }
      document.save(resultPath.toString());

    } catch (IOException ex) {
      Unchecked.rethrow(ex);
    }
  }

  private void drawText(PDPageContentStream content, float x, float y, String text) throws IOException {
    content.setNonStrokingColor(0, 0, 0); // black text
    content.beginText();
    content.setFont(PDType1Font.HELVETICA_BOLD, 12);
    content.newLineAtOffset(x, y);
    content.showText(text);
    content.endText();
  }

  private String imageName(PrintedCard card) {
    String name = StringUtils.stripAccents(card.card().name()) //
        .replace("/", "-") //
        .replace("\"", "");

    // when more than 1 variants, add 1 (start with 1)
    int variant = card.edition().cards().count(card.card()) > 1 //
        ? card.variation() + 1 //
        : card.variation();

    String suffix = variant > 0 ? "." + variant : "";
    return name + "." + card.edition().id() + suffix + ".jpg";
  }

  // paged list of cards to be printed from given decks, minus those already in given collection
  private List<Multimap<PrintedDeck, PrintedCard>> paged(Set<PrintedDeck> decks, Set<PrintedDeck> collection) {
    List<Multimap<PrintedDeck, PrintedCard>> result = new ArrayList<>();
    Multimap<PrintedDeck, PrintedCard> page = null;
    int counter = 0;
    for (PrintedDeck deck : decks) {
      for (PrintedCard card : deck.cards()) {
        if (collection.stream().anyMatch(d -> d.cards().stream().anyMatch(c -> c.card().equals(card.card())))) {
          continue;
        }
        if (counter++ % 9 == 0) {
          page = LinkedListMultimap.create();
          result.add(page);
        }
        page.put(deck, card);

        // FIXME print tokens & emblems
        if (card.card() instanceof DoubleFacedCard) {
          if (counter++ % 9 == 0) {
            page = LinkedListMultimap.create();
            result.add(page);
          }
          // FIXME print placeholder for doublesided cards
          page.put(deck, new PrintedCard(((DoubleFacedCard) card.card()).back(), card.edition(), card.variation()));
        }
      }
    }
    return result;
  }
}
