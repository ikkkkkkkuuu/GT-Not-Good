// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Relocated Commodore AbstractIterator used to parse bundled command definitions. */
abstract class AbstractIterator<T> implements Iterator<T> {
   private AbstractIterator.State state = AbstractIterator.State.NOT_READY;
   private T next;

   protected AbstractIterator() {
   }

   protected abstract T computeNext() throws Exception;

   protected final T endOfData() {
      this.state = AbstractIterator.State.DONE;
      return null;
   }

   @Override
   public final boolean hasNext() {
      if (this.state == AbstractIterator.State.FAILED) {
         throw new IllegalStateException();
      } else {
         switch (this.state) {
            case DONE:
               return false;
            case READY:
               return true;
            default:
               return this.tryToComputeNext();
         }
      }
   }

   private boolean tryToComputeNext() {
      this.state = AbstractIterator.State.FAILED;

      try {
         this.next = this.computeNext();
      } catch (Exception var2) {
         throw new RuntimeException("Exception whilst computing next value", var2);
      }

      if (this.state != AbstractIterator.State.DONE) {
         this.state = AbstractIterator.State.READY;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final T next() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         this.state = AbstractIterator.State.NOT_READY;
         T result = this.next;
         this.next = null;
         return result;
      }
   }

   public final T peek() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         return this.next;
      }
   }

   private static enum State {
      READY,
      NOT_READY,
      DONE,
      FAILED;
   }
}
// spotless:on
