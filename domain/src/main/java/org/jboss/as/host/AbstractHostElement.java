/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.host;

import java.util.Collection;
import org.jboss.as.domain.AbstractDomainUpdate;
import org.jboss.as.model.AbstractModel;
import org.jboss.as.model.AbstractModelElement;

/**
 * An object within the domain model.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractHostElement<E extends AbstractHostElement<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = 1L;

    protected AbstractHostElement() {
    }

    /** {@inheritDoc} */
    protected final void addToModel(final AbstractModel<?> model) throws IllegalArgumentException {
        if (model instanceof Host) {
            addToHost((Host) model);
        } else {
            throw new IllegalArgumentException("Domain element cannot be added to this model");
        }
    }

    protected void addToHost(final Host host) throws IllegalArgumentException {
        host.addElement(this);
    }

    /** {@inheritDoc} */
    public abstract Collection<? extends AbstractDomainUpdate<?>> getDifference(final E other);
}