package cc.blunet.mtg.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import cc.blunet.common.BaseEntity;

public abstract class Card extends BaseEntity<String> {

  public Card(String name) {
    super(name);
  }

  public String name() {
    return id();
  }

  public static final class SimpleCard extends Card {

    public SimpleCard(String name) {
      super(name);
    }
  }

  public abstract static class MultiCard extends Card {
    protected final List<SimpleCard> cards;

    public MultiCard(SimpleCard... cards) {
      this(Stream.of(cards).distinct().collect(toImmutableList()));
      checkState(cards.length == this.cards.size());
    }

    private MultiCard(List<SimpleCard> cards) {
      super(cards.stream().map(Card::name).collect(joining(" / ")));
      this.cards = checkNotNull(cards);
    }

    public Set<SimpleCard> cards() {
      return ImmutableSet.copyOf(cards);
    }
  }

  /**
   * A double-sided card's name is it's front side name.
   */
  public static final class DoubleFacedCard extends MultiCard {

    public DoubleFacedCard(SimpleCard front, SimpleCard back) {
      super(front, back);
    }

    public Card front() {
      return cards.get(0);
    }

    public Card back() {
      return cards.get(1);
    }
  }

  /**
   * A flip-card's name is it's front side name.
   */
  public static final class FlipCard extends MultiCard {

    public FlipCard(SimpleCard top, SimpleCard bottom) {
      super(top, bottom);
    }

    public SimpleCard top() {
      return cards.get(0);
    }

    public SimpleCard bottom() {
      return cards.get(1);
    }
  }

  /**
   * A split-card's name is composed of both cards.
   */
  public static final class SplitCard extends MultiCard {

    public SplitCard(SimpleCard left, SimpleCard right) {
      super(left, right);
    }

    public SplitCard(SimpleCard... cards) {
      super(cards);
    }

    public SimpleCard left() {
      return cards.get(0);
    }

    public SimpleCard right() {
      return cards.get(1);
    }
  }

  /**
   * An aftermath-card's name is composed of both cards.
   */
  public static final class AftermathCard extends MultiCard {

    public AftermathCard(SimpleCard left, SimpleCard right) {
      super(left, right);
    }

    public SimpleCard top() {
      return cards.get(0);
    }

    public SimpleCard bottom() {
      return cards.get(1);
    }
  }

  /**
   * Logically one card, consisting of two halves.
   */
  public static final class TwoPartCard extends Card {

    public TwoPartCard(String name, String part) {
      super(name + " (" + part + ")");
    }

    public String groupId() {
      return id().substring(0, id().lastIndexOf(' '));
    }
  }

  /**
   * Tokens are not in the deck or sideboard.
   */
  public static final class TokenCard extends Card {

    public TokenCard(String name) {
      super(name);
    }
  }
}
