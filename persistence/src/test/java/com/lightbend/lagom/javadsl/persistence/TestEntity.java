/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.lagom.javadsl.persistence;

import javax.inject.Inject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.lightbend.lagom.serialization.Jsonable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import akka.Done;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;

import com.lightbend.lagom.javadsl.persistence.testkit.SimulatedNullpointerException;
import org.pcollections.PSequence;

public class TestEntity extends PersistentEntity<TestEntity.Cmd, TestEntity.Evt, TestEntity.State> {

  public static interface Cmd extends Jsonable {
  }

  public static class Get implements Cmd, PersistentEntity.ReplyType<State> {
    private static Get instance = new Get();

    @JsonCreator
    public static Get instance() {
      return Get.instance;
    }

    private Get() {
    }
  }

  public static class Add implements Cmd, PersistentEntity.ReplyType<Evt> {
    private final String element;
    private final int times;

    public static Add of(String element) {
      return new Add(element, 1);
    }

    @JsonCreator
    public Add(String element, int times) {
      this.element = element;
      this.times = times;
    }

    public String getElement() {
      return element;
    }

    public int getTimes() {
      return times;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((element == null) ? 0 : element.hashCode());
      result = prime * result + times;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Add other = (Add) obj;
      if (element == null) {
        if (other.element != null)
          return false;
      } else if (!element.equals(other.element))
        return false;
      if (times != other.times)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Add [element=" + element + ", times=" + times + "]";
    }

  }

  public enum Mode {
    PREPEND, APPEND
  }

  public static class ChangeMode implements Cmd, PersistentEntity.ReplyType<Evt> {
    private final Mode mode;

    @JsonCreator
    public ChangeMode(Mode mode) {
      this.mode = mode;
    }

    public Mode getMode() {
      return mode;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ChangeMode other = (ChangeMode) obj;
      if (mode != other.mode)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "ChangeMode [mode=" + mode + "]";
    }

  }

  public static class UndefinedCmd implements Cmd, PersistentEntity.ReplyType<Done> {
    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      else
        return (getClass() == obj.getClass());
    }

    @Override
    public String toString() {
      return "UndefinedCmd";
    }
  }

  public static abstract class Evt implements AggregateEvent<Evt>, Jsonable {

    public static final int NUM_SHARDS = 4;

    public static final PSequence<AggregateEventTag<Evt>> aggregateTags = AggregateEventTag.shards(Evt.class,
        NUM_SHARDS); // second param is optional, defaults to the class name

    public abstract String getEntityId();

    @Override
    public AggregateEventTag<Evt> aggregateTag() {
      return AggregateEventTag.shard(Evt.class, NUM_SHARDS, getEntityId());
    }
  }

  public static class Appended extends Evt {
    private final String entityId;
    private final String element;

    @JsonCreator
    public Appended(String entityId, String element) {
      this.entityId = entityId;
      this.element = element;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    public String getElement() {
      return element;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Appended appended = (Appended) o;

      if (!entityId.equals(appended.entityId)) return false;
      return element.equals(appended.element);

    }

    @Override
    public int hashCode() {
      int result = entityId.hashCode();
      result = 31 * result + element.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Appended{" +
              "entityId='" + entityId + '\'' +
              ", element='" + element + '\'' +
              '}';
    }
  }

  public static class Prepended extends Evt {
    private final String entityId;
    private final String element;

    @JsonCreator
    public Prepended(String entityId, String element) {
      this.entityId = entityId;
      this.element = element;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    public String getElement() {
      return element;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Prepended prepended = (Prepended) o;

      if (!entityId.equals(prepended.entityId)) return false;
      return element.equals(prepended.element);

    }

    @Override
    public int hashCode() {
      int result = entityId.hashCode();
      result = 31 * result + element.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Prepended{" +
              "entityId='" + entityId + '\'' +
              ", element='" + element + '\'' +
              '}';
    }
  }

  public static class InPrependMode extends Evt {

    private final String entityId;

    @JsonCreator
    public InPrependMode(String entityId) {
      this.entityId = entityId;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InPrependMode that = (InPrependMode) o;

      return entityId.equals(that.entityId);

    }

    @Override
    public int hashCode() {
      return entityId.hashCode();
    }

    @Override
    public String toString() {
      return "InPrependMode{" +
              "entityId='" + entityId + '\'' +
              '}';
    }
  }

  public static class InAppendMode extends Evt {
    private final String entityId;

    @JsonCreator
    public InAppendMode(String entityId) {
      this.entityId = entityId;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InAppendMode that = (InAppendMode) o;

      return entityId.equals(that.entityId);

    }

    @Override
    public int hashCode() {
      return entityId.hashCode();
    }

    @Override
    public String toString() {
      return "InAppendMode{" +
              "entityId='" + entityId + '\'' +
              '}';
    }
  }

  public static class State implements Jsonable {

    public static final State EMPTY = new State(Mode.APPEND, ImmutableList.of());

    private final Mode mode;
    private final ImmutableList<String> elements;

    @JsonCreator
    public State(Mode mode, ImmutableList<String> elements) {
      this.mode = mode;
      this.elements = elements;
    }

    public ImmutableList<String> getElements() {
      return elements;
    }

    public State add(String elem) {
      List<String> newElements = new ArrayList<>(elements);
      if (mode == Mode.PREPEND)
        newElements.add(0, elem);
      else
        newElements.add(elem);
      return new State(mode, ImmutableList.copyOf(newElements));
    }

    public State prependMode() {
      return new State(Mode.PREPEND, elements);
    }

    public State appendMode() {
      return new State(Mode.APPEND, elements);
    }

    public Mode getMode() {
      return mode;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((elements == null) ? 0 : elements.hashCode());
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      State other = (State) obj;
      if (elements == null) {
        if (other.elements != null)
          return false;
      } else if (!elements.equals(other.elements))
        return false;
      if (mode != other.mode)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "State [mode=" + mode + ", elements=" + elements + "]";
    }

  }

  public static class GetAddress implements Cmd, PersistentEntity.ReplyType<Address> {
    public static GetAddress instance = new GetAddress();

    private GetAddress() {
    }
  }

  // TestProbe message
  public static class Snapshot {
    public final State state;

    Snapshot(State state) {
      this.state = state;
    }
  }

  //TestProbe message
  public static class AfterRecovery {
    public final State state;

    AfterRecovery(State state) {
      this.state = state;
    }
  }

  private final ActorSystem system;
  private final Optional<ActorRef> probe;

  @Inject
  public TestEntity(ActorSystem system) {
    this.system = system;
    this.probe = Optional.empty();
  }

  public TestEntity(ActorSystem system, ActorRef probe) {
    this.system = system;
    this.probe = Optional.ofNullable(probe);
  }

  @Override
  public Behavior initialBehavior(Optional<State> snapshotState) {

    if (snapshotState.isPresent())
      probe.ifPresent(p -> p.tell(new Snapshot(snapshotState.get()), ActorRef.noSender()));

    BehaviorBuilder b = newBehaviorBuilder(State.EMPTY);

    if (snapshotState.isPresent()) {
      b.setState(snapshotState.get());
    }

    b.setEventHandler(Appended.class, evt -> state().add(evt.getElement()));
    b.setEventHandler(Prepended.class, evt -> state().add(evt.getElement()));

    b.setEventHandlerChangingBehavior(InAppendMode.class, evt -> becomeAppending(behavior()));
    b.setEventHandlerChangingBehavior(InPrependMode.class, evt -> becomePrepending(behavior()));

    b.setReadOnlyCommandHandler(Get.class, (cmd, ctx) -> {
      ctx.reply(state());
    });

    b.setReadOnlyCommandHandler(GetAddress.class, (cmd, ctx) -> {
      ctx.reply(Cluster.get(system).selfAddress());
    });

    b.setCommandHandler(ChangeMode.class,
      (cmd, ctx) -> {
        if (state().getMode() == cmd.getMode()) {
          return ctx.done();
        } else if (cmd.getMode() == Mode.APPEND) {
          return ctx.thenPersist(new InAppendMode(entityId()), evt -> ctx.reply(evt));
        } else if (cmd.getMode() == Mode.PREPEND) {
          return ctx.thenPersist(new InPrependMode(entityId()), evt -> ctx.reply(evt));
        } else {
          throw new IllegalStateException();
        }
      });


    if (b.getState().getMode() == Mode.APPEND) {
      return becomeAppending(b.build());
    } else {
      return becomePrepending(b.build());
    }
  }

  private Behavior becomeAppending(Behavior current) {
    BehaviorBuilder b = current.builder();
    b.setState(current.state().appendMode());
    b.setCommandHandler(Add.class, (cmd, ctx) -> {
      // note that null should trigger NPE, for testing exception
        if (cmd.getElement() == null)
          throw new SimulatedNullpointerException();
      if (cmd.getElement().length() == 0) {
        ctx.invalidCommand("element must not be empty");
        return ctx.done();
      }
      Appended a = new Appended(entityId(), cmd.element.toUpperCase());
      if (cmd.getTimes() == 1)
        return ctx.thenPersist(a, evt -> ctx.reply(evt));
      else
        return ctx.thenPersistAll(fill(a, cmd.getTimes()), () -> ctx.reply(a));
    });
    return b.build();
  }

  private Behavior becomePrepending(Behavior current) {
    BehaviorBuilder b = current.builder();
    b.setState(current.state().prependMode());
    b.setCommandHandler(Add.class, (cmd, ctx) -> {
      if (cmd.getElement() == null || cmd.getElement() == "") {
        ctx.invalidCommand("element must not be empty");
        return ctx.done();
      }
      Prepended a = new Prepended(entityId(), cmd.element.toLowerCase());
      if (cmd.getTimes() == 1)
        return ctx.thenPersist(a, evt -> ctx.reply(evt));
      else
        return ctx.thenPersistAll(fill(a, cmd.getTimes()), () -> ctx.reply(a));
    });

    b.setCommandHandler(Add.class,
        (cmd, ctx) -> ctx.thenPersist(new Prepended(entityId(), cmd.element.toLowerCase()), evt -> ctx.reply(evt)));
    return b.build();
  }

  private <T> List<T> fill(T evt, int times) {
    List<T> events = new ArrayList<>();
    for (int i = 0; i < times; i++) {
      events.add(evt);
    }
    return events;
  }

  @Override
  public Behavior recoveryCompleted() {
    probe.ifPresent(p -> p.tell(new AfterRecovery(state()), ActorRef.noSender()));
    return behavior();
  }

}
